package utopia.echo.controller.vastai.vllm

import utopia.access.model.enumeration.Status.BadRequest
import utopia.annex.controller.RequestQueue
import utopia.annex.model.response.RequestNotSent.{RequestSendingFailed, RequestWasDeprecated}
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.disciple.controller.Gateway
import utopia.echo.controller.chat.BufferingChatRequestExecutor
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.controller.vastai.VastAiServiceProcess.VastAiServiceSettings
import utopia.echo.controller.vastai.vllm.VastAiVllmChatExecutor.{VastAiVllmChatExecutorSettings, maxRetries}
import utopia.echo.controller.vastai.vllm.VastAiVllmProcess.{VastAiVllmSettings, VastAiVllmSettingsFactory, VastAiVllmSettingsWrapper}
import utopia.echo.controller.vastai.{InstanceReuseLogic, SelectOffer}
import utopia.echo.model.enumeration.ModelParameter.ContextTokens
import utopia.echo.model.enumeration.ServiceState
import utopia.echo.model.llm.{LlmDesignator, LlmVramUse}
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.BufferedOpenAiChatCompletionRequest
import utopia.echo.model.request.vastai.DestroyInstance
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.vastai.instance.InstanceState.{Active, Loading, Unknown}
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.instance.{NewInstanceFoundation, VastAiInstance}
import utopia.echo.model.vastai.process.VastAiServiceState.Hosting
import utopia.echo.model.vastai.process.VastAiServiceState.VastAiServicePhase.{NotStarted, Serving, Stopping}
import utopia.echo.model.vastai.process.{VastAiVllmChatExecutorStatus, VastAiVllmProcessRecorder, VastAiVllmProcessorStatus}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.{AccessQueue, MappingFunnel, Scheduler}
import utopia.flow.async.process.WaitTarget.WaitDuration
import utopia.flow.async.process._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.ChangeResponsePriority.After
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now, Today}
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.mutable.{Pointer, Settable}

import java.net.ServerSocket
import java.nio.file.Path
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object VastAiVllmChatExecutor
{
	// ATTRIBUTES   --------------------
	
	private val maxRetries = 8
	
	val factory = new VastAiVllmChatExecutorFactory(VastAiVllmChatExecutorSettings.default)
	
	
	// IMPLICIT    ------------------------
	
	// Implicitly treats this object as a factory
	implicit def objectAsFactory(o: VastAiVllmChatExecutor.type): VastAiVllmChatExecutorFactory = o.factory
			
	
	// NESTED   -------------------------------
	
	trait VastAiVllmChatExecutorSettingsFactory[+Repr] extends VastAiVllmSettingsFactory[Repr]
	{
		// ABSTRACT    ------------------------
		
		/**
		 * @param coreInstanceCount Number of Vast AI instances to reserve immediately
		 *                          (or when the first request is received, depending on whether startup is lazy or not).
		 * @return Copy of this instance reserving the specified number of instances immediately.
		 */
		def startingWithInstances(coreInstanceCount: Int): Repr
		/**
		 * @param maxInstanceCount Maximum number of Vast AI instances to reserve
		 * @return A copy of this instance reserving at most the specified number of instances
		 */
		def usingAtMostInstances(maxInstanceCount: Int): Repr
		/**
		 * @param stopInstanceCount Maximum number of stopped instances kept in reserve for speeding up startup.
		 * @return Copy of this instance keeping the specified number of instances in reserve (in stopped state)
		 */
		def reservingInstances(stopInstanceCount: Int): Repr
		/**
		 * Specifies the number of Vast AI instances to acquire
		 * @param core Number of instances to activate immediately
		 * @param max Maximum number of instances that can be activated at any time
		 * @param reserve Number of instances to keep in reserve in stopped state.
		 * @return Copy of this instance applying the specified instance thresholds.
		 */
		def withInstanceCounts(core: Int, max: Int, reserve: Int): Repr
		
		/**
		 * @param queueSizeThreshold Number of queued requests at which an additional Vast AI
		 *                           instance should be acquired.
		 *
		 *                           Requests are queued while no instances are able to process requests,
		 *                           and also while all running instances are clearly too full.
		 *
		 *                           This threshold may stack / be applied multiple times;
		 *                           I.e. If this threshold is 20 requests and 40 requests are queued,
		 *                           activates 2 instances at once.
		 *
		 * @return Copy of this instance with the specified request queue-size threshold
		 * @see [[activatingInstancesAtPendingTokens]]
		 * @see [[acceleratingActivationAtPendingTokens]]
		 */
		def activatingInstancesAtQueueSize(queueSizeThreshold: Int): Repr
		/**
		 * @param pendingTokensThreshold Number of combined tokens (input + expected output) of ALL pending
		 *                               requests PER a single Vast AI instance, on average,
		 *                               at which an additional instance will be acquired.
		 *
		 *                               E.g. If we have 3 active instances with 5 K, 10 K, and 15 K pending tokens,
		 *                               this threshold would have to be 10 K in order for a 4th instance to be
		 *                               acquired.
		 *
		 *                               Note: This value doesn't stack. For reserving more than one instance at
		 *                               a time, the acceleration threshold must be reached instead.
		 *
		 * @return A copy of this instance with the specified activation (token) threshold.
		 * @see [[acceleratingActivationAtPendingTokens]]
		 * @see [[activatingInstancesAtQueueSize]]
		 */
		def activatingInstancesAtPendingTokens(pendingTokensThreshold: TokenCount): Repr
		/**
		 * @param pendingTokensThreshold Number of combined tokens (input + expected output) of ALL pending
		 *                               requests PER a single Vast AI instance, on average,
		 *                               at which additional instances will be acquired.
		 *
		 *                               E.g. If we have 3 active instances with 15 K, 20 K, and 25 K pending tokens,
		 *                               and one instance being activated (with 0 pending tokens),
		 *                               this threshold would have to be 15 K for a 5th instance to be activated.
		 *
		 *                               This value stacks, possibly triggering
		 *                               more than 2 instances being activated at once.
		 *                               On the other hand, the first activation is based on
		 *                               the activation threshold instead.
		 *
		 * @return A copy of this instance with the specified acceleration (token) threshold.
		 * @see [[activatingInstancesAtPendingTokens]]
		 */
		def acceleratingActivationAtPendingTokens(pendingTokensThreshold: TokenCount): Repr
		/**
		 * Specifies queue size and pending token thresholds, at which additional Vast AI instances will be acquired.
		 * @param queueSizeThreshold Number of queued requests, after which a new instance is acquired. May stack.
		 * @param pendingTokensThreshold Number of pending tokens (total input + expected output) on average per
		 *                               active instance, after which an additional instance will be acquired.
		 *                               Only applied while no new instances are being acquired / loaded.
		 * @param accelerationThreshold Number of pending tokens on average per instance (whether active or loading),
		 *                              at which an additional instance will be acquired.
		 *                              Applied while new instances are being loaded. May stack.
		 * @return A copy of this instance applying the specified instance activation thresholds.
		 */
		def withActivationThresholds(queueSizeThreshold: Int, pendingTokensThreshold: TokenCount,
		                             accelerationThreshold: TokenCount): Repr
		
		/**
		 * Limits the number of active HTTP connections per a single Vast AI instance.
		 * @param connectionsPerInstance Maximum number of HTTP connections to allow to any single rented instance.
		 *                               A larger number allows for wider parallelism, but may use more resources.
		 * @return A copy of this instance using the specified connection limit.
		 */
		def withMaxHttpConnectionsPerInstance(connectionsPerInstance: Int): Repr
		
		/**
		 * Specifies the default context size to assume.
		 * Note: If not specified, incoming requests may fail or may be assigned to the backup executor, if applicable.
		 * @param contextSize Request context size to assume when no instances have been acquired or even loading.
		 *                    None if no context size should be assumed (not recommended).
		 * @return Copy of this instance assuming the specified context size before other information is available.
		 */
		def withDefaultContextSize(contextSize: Option[TokenCount]): Repr
		/**
		 * @param safetyMargin Safety margin added to context size calculations.
		 *
		 *                     If the request is at the edge of a processor's context size limit,
		 *                     may transfer it to another processor with a larger maximum context size.
		 *
		 * @return A copy of this instance using the specified safety margin.
		 */
		def withContextSafetyMargin(safetyMargin: TokenCount): Repr
		
		/**
		 * @param backupExecutor A request processing logic to use for requests that are too large
		 *                       for this executor to process. I.e. for requests that require a context size larger
		 *                       than any of the rented Vast AI instances can process.
		 * @return A copy of this instance using the specified backup processing logic.
		 */
		def usingBackupExecutor(backupExecutor: BufferingChatRequestExecutor[BufferedOpenAiReply]): Repr
		
		/**
		 * @param recorder An interface for recording each rented Vast AI instance.
		 * @return A copy of this instance informing the specified recorder about each acquired and closed instance.
		 */
		def recordingUsing(recorder: VastAiVllmProcessRecorder): Repr
		
		/**
		 * @param setupTimeout Timeout for the setup / instance loading process, applying to instance reuse.
		 *                     If this timeout is reached, the instance
		 *                     won't be considered viable for reservation / reuse.
		 *
		 *                     Use this to filter out unresponsive or slow instances from the reuse pool.
		 *
		 * @return A copy of this instance applying the specified timeout value for instance reuse.
		 */
		def onlyReusingInstancesUntil(setupTimeout: Duration): Repr
		/**
		 * @param idleShutdownThreshold A time threshold at which completely idle processes are stopped,
		 *                              destroying or stopping the underlying Vast AI instance(s).
		 *
		 *                              Lower values mean lower costs during low-demand periods,
		 *                              but may slow down the process and incur larger download & setup costs
		 *                              if the demand rises sharply afterwards.
		 *
		 * @return A copy of this instance closing idle processors after the specified time threshold.
		 */
		def closingIdleProcessorsAfter(idleShutdownThreshold: Duration): Repr
		/**
		 * @param partiallyUsedShutdownThreshold A time threshold at which partially used processors are stopped,
		 *                                       destroying or stopping the underlying Vast AI instance(s).
		 *
		 *                                       Lower values mean lower costs during low-demand periods,
		 *                                       but may slow down the process and incur larger download & setup costs
		 *                                       if the demand rises sharply afterwards.
		 *
		 * @return A copy of this instance closing partially used processors after the specified time threshold.
		 * @see [[closingIdleProcessorsAfter]]
		 */
		def closingPartiallyUsedProcessorsAfter(partiallyUsedShutdownThreshold: Duration): Repr
		
		/**
		 * @param thinks A function that receives name of the hosted LLM and yields whether that model uses
		 *               "thinking" / "reflection".
		 * @return A copy of this instance using the specified logic to detect thinking models.
		 */
		def detectingThinkUsing(thinks: String => Boolean): Repr
		
		/**
		 * @param startupLazily Whether this executor should only start acquiring Vast AI instances
		 *                      when the first request is received.
		 *                      False if the first instances should be acquired immediately when
		 *                      this executor is constructed.
		 * @return A copy of this instance applying the specified startup style
		 */
		def withStartupIsLazy(startupLazily: Boolean): Repr
		/**
		 * @param preserveStopped Whether stopped instances should be preserved when this executor is stopped.
		 *                        False if all stopped instances should be destroyed at that point.
		 * @return A copy of this instance applying the specified preservation logic.
		 */
		def withStoppedInstancePreservation(preserveStopped: Boolean): Repr
		
		
		// COMPUTED ------------------------
		
		/**
		 * @return A copy of this instance that assumes that all used LLMs use the "thinking" / "reflection" mode.
		 */
		def assumingAllModelsThink = detectingThinkUsing { _ => true }
		/**
		 * @return A copy of this instance that assumes that
		 *         none of the used LLMs use the "thinking" / "reflection" mode.
		 */
		def assumingNoModelsThink = detectingThinkUsing { _ => false }
		
		/**
		 * @return A copy of this instance that starts acquiring Vast AI instances only at the point where the
		 *         first request is received.
		 * @see [[withDefaultContextSize]]
		 */
		def startingLazily = withStartupIsLazy(true)
		/**
		 * @return A copy of this instance that preserves all stopped instances upon shutdown.
		 *         Note: The instances may need to be destroyed later manually,
		 *         or by a consequent process utilizing those same instances.
		 */
		def preservingStoppedInstances = withStoppedInstancePreservation(true)
		
		
		// OTHER    ------------------------
		
		/**
		 * Specifies the default context size to assume.
		 * Note: If not specified, incoming requests may fail or may be assigned to the backup executor, if applicable.
		 * @param contextSize Request context size to assume when no instances have been acquired or even loading.
		 * @return Copy of this instance assuming the specified context size before other information is available.
		 */
		def withDefaultContextSize(contextSize: TokenCount): Repr = withDefaultContextSize(Some(contextSize))
		
		/**
		 * @param thinkingModels Names of the LLMs that use "thinking" / "reflection" mode.
		 * @return A copy of this instance that assumes only the specified set of LLMs
		 *         enter "thinking" / "reflection" mode.
		 */
		def assumingCertainModelsThink(thinkingModels: Set[String]): Repr = detectingThinkUsing(thinkingModels.contains)
		
		/**
		 * @param logDirectory Directory where debug log entries will be placed.
		 * @param exc Implicit execution context
		 * @param scheduler Implicit scheduler
		 * @param log Implicit logging implementation for logging errors (default = System.err).
		 * @return A copy of this instance debug-logging to a file in the specified directory.
		 */
		def withDebugLogDirectory(logDirectory: Path)
		                         (implicit exc: ExecutionContext, scheduler: Scheduler, log: Logger = SysErrLogger): Repr =
			debugLoggingWith(KeptOpenWriter(30.seconds).to((logDirectory/s"$Today-Vast-AI-log.txt").unique))
	}
	
	object VastAiVllmChatExecutorSettings
	{
		// ATTRIBUTES   --------------------------
		
		lazy val default = VastAiVllmChatExecutorSettings()
		
		
		// IMPLICIT ------------------------------
		
		implicit def asVllmSettings(settings: VastAiVllmChatExecutorSettings): VastAiVllmSettings =
			settings.vllmSettings
		implicit def asServiceSettings(settings: VastAiVllmChatExecutorSettings): VastAiServiceSettings =
			settings.vllmSettings.serviceSettings
	}
	/**
	 * Specifies settings accepted by Vast AI vLLM chat executors
	 * @param vllmSettings Settings to apply to vLLM hosting and Vast AI renting logic.
	 *                     Default = use default settings, applying label "chat-executor" to all acquired instances.
	 * @param coreInstanceCount Number of Vast AI instances to reserve immediately
	 *                          (or when the first request is received, depending on whether startup is lazy or not).
	 *                          Default = 1.
	 * @param maxInstanceCount Maximum number of Vast AI instances to reserve. Default = 4.
	 * @param stopInstanceCount Maximum number of stopped instances kept in reserve for speeding up startup.
	 *                          Default = 0.
	 * @param instanceActivationQueueSizeThreshold Number of queued requests,
	 *                                             after which a new instance is acquired. May stack.
	 *                                             Default = 24.
	 * @param instanceActivationPendingTokensThreshold Number of pending tokens (total input + expected output)
	 *                                                 on average per active instance,
	 *                                                 after which an additional instance will be acquired.
	 *                                                 Only applied while no new instances are being acquired / loaded.
	 * @param instanceAccelerationPendingTokensThreshold Number of pending tokens on average per instance
	 *                                                   (whether active or loading),
	 *                                                   at which an additional instance will be acquired.
	 *                                                   Applied while new instances are being loaded. May stack.
	 * @param maxConnectionsPerInstance Maximum number of HTTP connections to allow to any single rented instance.
	 *                                  A larger number allows for wider parallelism, but may use more resources.
	 *                                  Default = 28.
	 * @param defaultContextSize Request context size to assume when no instances have been acquired or even loading.
	 *                           None if no context size should be assumed (default, but not recommended).
	 * @param contextSafetyMargin Safety margin added to context size calculations.
	 *
	 *                            If the request is at the edge of a processor's context size limit,
	 *                            may transfer it to another processor with a larger maximum context size.
	 * @param backupExecutor A request processing logic to use for requests that are too large
	 *                       for this executor to process. I.e. for requests that require a context size larger
	 *                       than any of the rented Vast AI instances can process.
	 *                       None if no backup-executor should be used (default).
	 * @param recorder An interface for recording each rented Vast AI instance. None if not used (default).
	 * @param reuseSetupTimeout Timeout for the setup / instance loading process, applying to instance reuse.
	 *                          If this timeout is reached, the instance
	 *                          won't be considered viable for reservation / reuse.
	 *
	 *                          Use this to filter out unresponsive or slow instances from the reuse pool.
	 *
	 *                          Default = 5 minutes.
	 * @param idleShutdownThreshold A time threshold at which completely idle processes are stopped,
	 *                            destroying or stopping the underlying Vast AI instance(s).
	 *
	 *                            Lower values mean lower costs during low-demand periods,
	 *                            but may slow down the process and incur larger download & setup costs
	 *                            if the demand rises sharply afterwards.
	 *
	 *                            Default = 15 minutes.
	 * @param partialUseShutdownThreshold A time threshold at which partially used processors are stopped,
	 *                                    destroying or stopping the underlying Vast AI instance(s).
	 *                                    Default = 25 minutes.
	 * @param thinks A function that receives name of the hosted LLM and yields whether that model uses
	 *               "thinking" / "reflection".
	 *               Default = always yield false.
	 * @param startsLazily Whether this executor should only start acquiring Vast AI instances
	 *                     when the first request is received.
	 *                     False if the first instances should be acquired immediately when
	 *                     this executor is constructed.
	 *                     Default = false.
	 * @param preservesStoppedInstances Whether stopped instances should be preserved when this executor is stopped.
	 *                                  False if all stopped instances should be destroyed at that point.
	 *                                  Default = false.
	 */
	case class VastAiVllmChatExecutorSettings(vllmSettings: VastAiVllmSettings = VastAiVllmSettings.default.withLabel("chat-executor"),
	                                          coreInstanceCount: Int = 1, maxInstanceCount: Int = 4,
	                                          stopInstanceCount: Int = 0,
	                                          instanceActivationQueueSizeThreshold: Int = 24,
	                                          instanceActivationPendingTokensThreshold: TokenCount = 24000,
	                                          instanceAccelerationPendingTokensThreshold: TokenCount = 48000,
	                                          maxConnectionsPerInstance: Int = 28,
	                                          defaultContextSize: Option[TokenCount] = None,
	                                          contextSafetyMargin: TokenCount = 64,
	                                          backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]] = None,
	                                          recorder: Option[VastAiVllmProcessRecorder] = None,
	                                          reuseSetupTimeout: Duration = 5.minutes,
	                                          idleShutdownThreshold: Duration = 15.minutes,
	                                          partialUseShutdownThreshold: Duration = 25.minutes,
	                                          thinks: String => Boolean = { _ => false },
	                                          startsLazily: Boolean = false, preservesStoppedInstances: Boolean = false)
		extends VastAiVllmChatExecutorSettingsFactory[VastAiVllmChatExecutorSettings]
			with VastAiVllmSettingsWrapper[VastAiVllmChatExecutorSettings]
	{
		// IMPLEMENTED  ----------------------------
		
		override def startingWithInstances(coreInstanceCount: Int): VastAiVllmChatExecutorSettings =
			copy(coreInstanceCount = coreInstanceCount)
		override def usingAtMostInstances(maxInstanceCount: Int): VastAiVllmChatExecutorSettings =
			copy(maxInstanceCount = maxInstanceCount)
		override def reservingInstances(stopInstanceCount: Int): VastAiVllmChatExecutorSettings =
			copy(stopInstanceCount = stopInstanceCount)
		override def withInstanceCounts(core: Int, max: Int, reserve: Int): VastAiVllmChatExecutorSettings =
			copy(coreInstanceCount = core, maxInstanceCount = max, stopInstanceCount = reserve)
		
		override def activatingInstancesAtQueueSize(queueSizeThreshold: Int): VastAiVllmChatExecutorSettings =
			copy(instanceActivationQueueSizeThreshold = queueSizeThreshold)
		override def activatingInstancesAtPendingTokens(pendingTokensThreshold: TokenCount): VastAiVllmChatExecutorSettings =
			copy(instanceActivationPendingTokensThreshold = pendingTokensThreshold)
		override def acceleratingActivationAtPendingTokens(pendingTokensThreshold: TokenCount): VastAiVllmChatExecutorSettings =
			copy(instanceAccelerationPendingTokensThreshold = pendingTokensThreshold)
		override def withActivationThresholds(queueSizeThreshold: Int, pendingTokensThreshold: TokenCount,
		                                      accelerationThreshold: TokenCount): VastAiVllmChatExecutorSettings =
			copy(instanceActivationQueueSizeThreshold = queueSizeThreshold,
				instanceActivationPendingTokensThreshold = pendingTokensThreshold,
				instanceAccelerationPendingTokensThreshold = accelerationThreshold)
		
		override def withMaxHttpConnectionsPerInstance(connectionsPerInstance: Int): VastAiVllmChatExecutorSettings =
			copy(maxConnectionsPerInstance = connectionsPerInstance)
		
		override def withDefaultContextSize(contextSize: Option[TokenCount]): VastAiVllmChatExecutorSettings =
			copy(defaultContextSize = contextSize)
		override def withContextSafetyMargin(safetyMargin: TokenCount): VastAiVllmChatExecutorSettings =
			copy(contextSafetyMargin = safetyMargin)
		
		override def usingBackupExecutor(backupExecutor: BufferingChatRequestExecutor[BufferedOpenAiReply]): VastAiVllmChatExecutorSettings =
			copy(backupExecutor = Some(backupExecutor))
		
		override def recordingUsing(recorder: VastAiVllmProcessRecorder): VastAiVllmChatExecutorSettings =
			copy(recorder = Some(recorder))
		
		override def onlyReusingInstancesUntil(setupTimeout: Duration): VastAiVllmChatExecutorSettings =
			copy(reuseSetupTimeout = setupTimeout)
		override def closingIdleProcessorsAfter(idleShutdownThreshold: Duration): VastAiVllmChatExecutorSettings =
			copy(idleShutdownThreshold = idleShutdownThreshold)
		override def closingPartiallyUsedProcessorsAfter(partiallyUsedShutdownThreshold: Duration): VastAiVllmChatExecutorSettings =
			copy(partialUseShutdownThreshold = partiallyUsedShutdownThreshold)
		
		override def detectingThinkUsing(thinks: String => Boolean): VastAiVllmChatExecutorSettings =
			copy(thinks = thinks)
		
		override def withStartupIsLazy(startupLazily: Boolean): VastAiVllmChatExecutorSettings =
			copy(startsLazily = startupLazily)
		override def withStoppedInstancePreservation(preserveStopped: Boolean): VastAiVllmChatExecutorSettings =
			copy(preservesStoppedInstances = preserveStopped)
		
		override def withSettings(settings: VastAiVllmSettings): VastAiVllmChatExecutorSettings =
			copy(vllmSettings = settings)
	}
	
	trait VastAiVllmChatExecutorSettingsWrapper[+Repr] extends VastAiVllmChatExecutorSettingsFactory[Repr]
		with VastAiVllmSettingsWrapper[Repr]
	{
		// ABSTRACT ------------------------------
		
		/**
		 * @return Applied chat-executor settings
		 */
		def executorSettings: VastAiVllmChatExecutorSettings
		/**
		 * @param settings New chat-executor settings to apply
		 * @return Copy of this instance applying the specified settings
		 */
		def withSettings(settings: VastAiVllmChatExecutorSettings): Repr
		
		
		// IMPLEMENTED  --------------------------
		
		override def vllmSettings: VastAiVllmSettings = executorSettings.vllmSettings
		
		override def startingWithInstances(coreInstanceCount: Int): Repr =
			mapExecutorSettings { _.startingWithInstances(coreInstanceCount) }
		override def usingAtMostInstances(maxInstanceCount: Int): Repr =
			mapExecutorSettings { _.usingAtMostInstances(maxInstanceCount) }
		override def reservingInstances(stopInstanceCount: Int): Repr =
			mapExecutorSettings { _.reservingInstances(stopInstanceCount) }
		override def withInstanceCounts(core: Int, max: Int, reserve: Int): Repr =
			mapExecutorSettings { _.withInstanceCounts(core, max, reserve) }
		override def activatingInstancesAtQueueSize(queueSizeThreshold: Int): Repr =
			mapExecutorSettings { _.activatingInstancesAtQueueSize(queueSizeThreshold) }
		override def activatingInstancesAtPendingTokens(pendingTokensThreshold: TokenCount): Repr =
			mapExecutorSettings { _.activatingInstancesAtPendingTokens(pendingTokensThreshold) }
		override def acceleratingActivationAtPendingTokens(pendingTokensThreshold: TokenCount): Repr =
			mapExecutorSettings { _.acceleratingActivationAtPendingTokens(pendingTokensThreshold) }
		override def withActivationThresholds(queueSizeThreshold: Int, pendingTokensThreshold: TokenCount,
		                                      accelerationThreshold: TokenCount): Repr =
			mapExecutorSettings {
				_.withActivationThresholds(queueSizeThreshold, pendingTokensThreshold, accelerationThreshold) }
		override def withMaxHttpConnectionsPerInstance(connectionsPerInstance: Int): Repr =
			mapExecutorSettings { _.withMaxHttpConnectionsPerInstance(connectionsPerInstance) }
		override def withDefaultContextSize(contextSize: Option[TokenCount]): Repr =
			mapExecutorSettings { _.withDefaultContextSize(contextSize) }
		override def withContextSafetyMargin(safetyMargin: TokenCount): Repr =
			mapExecutorSettings { _.withContextSafetyMargin(safetyMargin) }
		override def usingBackupExecutor(backupExecutor: BufferingChatRequestExecutor[BufferedOpenAiReply]): Repr =
			mapExecutorSettings { _.usingBackupExecutor(backupExecutor) }
		override def recordingUsing(recorder: VastAiVllmProcessRecorder): Repr =
			mapExecutorSettings { _.recordingUsing(recorder) }
		override def onlyReusingInstancesUntil(setupTimeout: Duration): Repr =
			mapExecutorSettings { _.onlyReusingInstancesUntil(setupTimeout) }
		override def closingIdleProcessorsAfter(idleShutdownThreshold: Duration): Repr =
			mapExecutorSettings { _.closingIdleProcessorsAfter(idleShutdownThreshold) }
		override def closingPartiallyUsedProcessorsAfter(partiallyUsedShutdownThreshold: Duration): Repr =
			mapExecutorSettings { _.closingPartiallyUsedProcessorsAfter(partiallyUsedShutdownThreshold) }
		override def detectingThinkUsing(thinks: String => Boolean): Repr =
			mapExecutorSettings { _.detectingThinkUsing(thinks) }
		override def withStartupIsLazy(startupLazily: Boolean): Repr =
			mapExecutorSettings { _.withStartupIsLazy(startupLazily) }
		override def withStoppedInstancePreservation(preserveStopped: Boolean): Repr =
			mapExecutorSettings { _.withStoppedInstancePreservation(preserveStopped) }
		
		override def withSettings(settings: VastAiVllmSettings): Repr = mapExecutorSettings { _.withSettings(settings) }
		
		
		// OTHER    ----------------------------
		
		def mapExecutorSettings(f: Mutate[VastAiVllmChatExecutorSettings]): Repr = withSettings(f(executorSettings))
	}
	
	class VastAiVllmChatExecutorFactory(override val executorSettings: VastAiVllmChatExecutorSettings)
		extends VastAiVllmChatExecutorSettingsWrapper[VastAiVllmChatExecutorFactory]
	{
		// IMPLEMENTED  -----------------------
		
		override def withSettings(settings: VastAiVllmChatExecutorSettings): VastAiVllmChatExecutorFactory =
			new VastAiVllmChatExecutorFactory(settings)
			
		
		// OTHER    ---------------------------
		
		/**
		 * @param selectOffer Logic for selecting Vast AI offers to take
		 * @param modelSize Size of the used model, including how much VRAM should be reserved for requests.
		 *                  If multiple models are used, the size of the largest model should be returned.
		 * @param assumedVram VRAM amount assumed before any instances have been reserved.
		 *                    Used for calculating the default maximum context size.
		 * @param chooseImage A function for choosing the image or Vast AI template to use.
		 *                    Accepts the selected offer and the applied maximum context size, yields:
		 *                          1. Instance-creation settings
		 *                          1. Expected initial vLLM service state at the remote instance
		 *                          1. Name of the model to start vLLM with.
		 *                             Optional if vLLM is started automatically by the image / template.
		 * @param exc Implicit execution context
		 * @param scheduler Implicit Scheduler
		 * @param vastAiClient Implicit Vast AI -interfacing client
		 * @param log Implicit logging implementation
		 * @return A new chat-executor
		 */
		def apply(selectOffer: SelectOffer, modelSize: LlmVramUse, assumedVram: ByteCount)
		         (chooseImage: (Offer, TokenCount) => (NewInstanceFoundation, ServiceState, String))
		         (implicit exc: ExecutionContext, scheduler: Scheduler, vastAiClient: VastAiApiClient,
		          log: Logger) =
			new VastAiVllmChatExecutor(selectOffer, modelSize, assumedVram)(chooseImage)
	}
}

/**
 * Executes buffered chat requests using multiple Vast AI instances + vLLM servers.
 * @param selectOffer Logic for selecting Vast AI offers to take
 * @param modelSize Size of the used model, including how much VRAM should be reserved for requests.
 *                  If multiple models are used, the size of the largest model should be returned.
 * @param assumedVram VRAM amount assumed before any instances have been reserved.
 *                    Used for calculating the default maximum context size.
 * @param settings Applied settings. Default = use default settings.
 * @param chooseImage A function for choosing the image or Vast AI template to use.
 *                    Accepts the selected offer and the applied maximum context size, yields:
 *                          1. Instance-creation settings
 *                          1. Expected initial vLLM service state at the remote instance
 *                          1. Name of the model to start vLLM with.
 *                             Optional if vLLM is started automatically by the image / template.
 * @param exc Implicit execution context
 * @param vastAiClient Implicit Vast AI -interfacing client
 * @param log Implicit logging implementation
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
class VastAiVllmChatExecutor(selectOffer: SelectOffer, modelSize: LlmVramUse, assumedVram: ByteCount,
                             settings: VastAiVllmChatExecutorSettings = VastAiVllmChatExecutorSettings.default)
                            (chooseImage: (Offer, TokenCount) => (NewInstanceFoundation, ServiceState, String))
                            (implicit exc: ExecutionContext, scheduler: Scheduler, vastAiClient: VastAiApiClient,
                             log: Logger)
	extends BufferingChatRequestExecutor[BufferedOpenAiReply] with Breakable
{
	// ATTRIBUTES   -----------------------
	
	private val gateway = Gateway(maxConnectionsPerRoute = settings.maxConnectionsPerInstance,
		maxConnectionsTotal = settings.maxConnectionsPerInstance * settings.maxInstanceCount,
		disableTrustStoreVerification = true)
	/**
	 * A pointer for acquiring unique port numbers
	 */
	private val portCounter = {
		// Continuously generates new port numbers
		val p = Volatile(18001)
		Iterator.continually { p.getAndUpdate { _ + 1 } }
			// Makes sure only to yield available ports
			.filter { port => Try { new ServerSocket(port).close() }.isSuccess }
	}
	
	/**
	 * A pointer that contains IDs of instances that were only stopped instead of destroyed.
	 * These are reused with priority and may be destroyed when this process completes.
	 */
	private val stoppedInstanceIdsP = Volatile.emptySeq[Int]
	
	private val vllmSettings = settings.vllmSettings.usingGateway(gateway)
		.reusingInstancesWith({
			// Case: Preserves some instances instead of destroying them
			//       => Applies reuse logic that uses those stopped instances
			if (settings.stopInstanceCount > 0) {
				val reuseStopped = ReuseStoppedInstances.onlyReusable.filter { instances =>
					if (instances.isEmpty)
						instances
					else {
						val stoppedIds = stoppedInstanceIdsP.value
						// Case: Using a custom label => Also reuses instances with that label
						settings.instanceLabel.ifNotEmpty match {
							case Some(label) =>
								instances.filter { instance =>
									instance.label == label || stoppedIds.contains(instance.id)
								}
							case None => instances.filter { instance => stoppedIds.contains(instance.id) }
						}
					}
				}
				reuseStopped || settings.instanceReuseLogic
			}
			// Case: Destroys all instances by default => Applies reuse logic from the received settings, only
			else
				settings.instanceReuseLogic
		})
		// Rotates the used local ports
		.withPortForwarding(View {
			val remotePort = settings.forwardedPorts match {
				case Some(ports) => ports.value.second
				case None => 8000
			}
			Pair(portCounter.next(), remotePort)
		})
		// Limits the applied status check frequency
		.updatingStatusEvery(settings.statusCheckInterval max (settings.maxInstanceCount * 2).seconds)
		.withDestructionLogic { (instance, runTime, apiHosted) =>
			// Checks the custom logic first
			if (settings.shouldDestroy(instance, runTime, apiHosted)) {
				// Case: Stopping is not enabled or already requested to stop without keeping stopped instances
				//       => Destroys all instances
				if (settings.stopInstanceCount <= 0 || (stopFlag.isSet && !settings.preservesStoppedInstances))
					true
				// Case: There's still space in reserved / stopped instances
				//       => Stops the instance unless the instance status/state is not promising
				else if (stoppedInstanceIdsP.value.size < settings.stopInstanceCount) {
					val stops = instance.status.actual.value match {
						case Loading | Active => apiHosted || runTime < settings.reuseSetupTimeout
						case _: Unknown => apiHosted
						case _ => false
					}
					// Case: Stops the instance instead of destroying it => Remembers the instance ID
					if (stops)
						stoppedInstanceIdsP :+= instance.id
					!stops
				}
				// Case: Enough reserved instances as is => Destroys this instance
				else
					true
			}
			// Case: Custom logic override => Stops the instance instead of destroying it.
			//                                Also, these don't count towards the reserved instances.
			else
				false
		}
	
	/**
	 * Maximum context size applied by default / before there are any instances reserved.
	 * The actual maximum context size is based on [[modelSize]] and available VRAM.
	 */
	private val defaultMaxContextSize = contextSizeOn(assumedVram)
	
	/**
	 * Caches the generated LLM designators
	 */
	private val llmCache = Cache { model: String => LlmDesignator(model, thinks = settings.thinks(model)) }
	/**
	 * Set to true once this executor should terminate
	 */
	private val stopFlag = Settable()
	
	/**
	 * Counts the number of consecutive failures to acquire a Vast AI instance.
	 * If this reaches 50, this executor stops.
	 */
	private val consecutiveGetInstanceFailuresP = Volatile(0)
	
	/**
	 * Contains requests received while no processors were available.
	 * Cleared once processors become available.
	 */
	private val _queue = Volatile.eventful.emptySeq[QueuedTask]
	private val hasQueueFlag = _queue.nonEmptyFlag
	
	/**
	 * Contains the number of instances that should be started at any time.
	 * This is increased based on demand, and decreased when instances become idle.
	 */
	private val targetInstanceCountP = Volatile.eventful(if (settings.startsLazily) 0 else settings.coreInstanceCount)
	/**
	 * A decreasing counter for request tokens that triggers a target instance count update when it reaches 0.
	 */
	private val pendingTokensCheckThresholdP = Volatile(0)
	/**
	 * A decreasing counter for queued requests that triggers a target instance count update when it reaches 0.
	 */
	private val queueSizeCheckThresholdP = Volatile(0)
	
	/**
	 * Used for limiting instance-creation to one instance at a time
	 */
	private val createInstanceAccess = new AccessQueue(())
	
	/**
	 * Contains the utilized processors.
	 */
	private val processorsP = Volatile.eventful.emptySeq[Processor]
	/**
	 * A pointer that lists the processors that are/were usable.
	 * Updated (manually) whenever API hosting starts or ends.
	 */
	private val usableProcessorsP = CopyOnDemand { processors.filter { _.usable } }
	
	/**
	 * A pointer that contains the maximum context size of the largest available Vast AI instance,
	 * or the default context size.
	 *
	 * Needs to be updated manually as processes become available or unavailable.
	 */
	private val maxContextSizeP = CopyOnDemand[TokenCount] {
		processors.iterator.filter { _.phase < Stopping }.flatMap { _.maxContextSize }.maxOption
			.getOrElse(defaultMaxContextSize)
	}
	/**
	 * A pointer that contains the maximum context size of the largest available Vast AI instance,
	 * or the default context size.
	 */
	// A public-facing version of maxContextSizeP. Omits the safety margin, which is added to the incoming requests.
	val maxContextSizePointer = maxContextSizeP.lightMap { _ - settings.contextSafetyMargin * 2 - 1 }
	
	/**
	 * A process that shuts down idle and partially used processors.
	 * None if no such process is needed.
	 * Must be restarted at times.
	 */
	private val idleShutdownProcess = {
		if (settings.idleShutdownThreshold.isFinite || settings.partialUseShutdownThreshold.isFinite)
			Some(LoopingProcess.restartable.after(settings.idleShutdownThreshold) { _ =>
				// Checks whether any processes are currently idle or only partially used
				val now = Now.toInstant
				val idleThreshold = now - settings.idleShutdownThreshold
				val (loading, active) = processors.divideBy { p => p.phase == Serving && !p.wasRequestedToStop }
					.toTuple
				val (remaining, idle) = active.divideBy { p => p.lastRequestTime < idleThreshold && p.isEmpty }.toTuple
				
				// Case: Some processes were idle => Stops them and reduces the target count accordingly
				if (idle.nonEmpty) {
					targetInstanceCountP.update { target => (target - idle.size - loading.size) max 0 }
					debugLog(s"Shutting down ${ idle.size } idle processors and ${ loading.size } loading processors")
					(idle.iterator ++ loading)
						.foreach { _.stop().forFailure { log(_, "Failure while stopping an idle processor") } }
					queueSizeCheckThresholdP.value = 0
					pendingTokensCheckThresholdP.value = 0
				}
				// Case: No idle processes => Checks whether some have not been fully utilized for some time
				else if (remaining.size > settings.coreInstanceCount ||
					(loading.nonEmpty && remaining.size == settings.coreInstanceCount))
				{
					lazy val partialUseThreshold = now - settings.partialUseShutdownThreshold
					remaining.find { p => p.notFullyUtilized && p.lastPendingEndTime < partialUseThreshold }
						// Case: A partially used processor found
						.foreach { partiallyUsedProcessor =>
							// Shuts down the loading processes (we don't need more processes at this time)
							if (loading.nonEmpty) {
								debugLog(s"Shutting down ${ loading.size } loading processors (not enough demand)")
								targetInstanceCountP.update { target => (target - loading.size) max 0 }
								loading.foreach {
									_.stop().forFailure { log(_, "Failure while stopping a loading processor") }
								}
							}
							
							// Makes sure we're really above the core processor count
							// before shutting down active processes
							val shouldTerminateActive = targetInstanceCountP.mutate { target =>
								if (target > settings.coreInstanceCount)
									true -> (target - 1)
								else
									false -> target
							}
							if (shouldTerminateActive) {
								debugLog("Shutting down a partially used processor")
								// Terminates the partially used instance
								partiallyUsedProcessor.stop()
									.forFailure { log(_, "Failure while stopping a partially used processor") }
								queueSizeCheckThresholdP.value = 0
								pendingTokensCheckThresholdP.value = 0
							}
						}
				}
				
				// Schedules the next check
				val nextCheck = Pair[Option[Instant]](
					remaining.iterator.map { _.lastRequestTime }.minOption.map { _ + settings.idleShutdownThreshold },
					remaining.iterator.filter { _.notFullyUtilized }.map { _.lastPendingEndTime }.minOption
						.map { _ + settings.partialUseShutdownThreshold })
					.iterator.flatten.minOption.getOrElse { now + settings.idleShutdownThreshold }
				Some(nextCheck)
			})
		// Case: Idle shutdown is disabled => No process needed
		else
			None
	}
	
	/**
	 * Contains the future of the current / latest queue-clearance process.
	 * Used for limiting clearing to a single process.
	 */
	private val clearQueueFutureP = Volatile(Future.successful(true))
	/**
	 * A listener for usable processors, which activates queue-clearing, if possible & appropriate.
	 */
	private val clearQueueListener: ChangeListener[Seq[Processor]] = ChangeListener[Seq[Processor]] { e =>
		// Case: Processors are now available => Prepares to clear the queue
		if (e.newValue.exists { _.usable })
			clearQueueFutureP.mutate { f =>
				// Case: Previous clearance had completed => Starts a new clearance process
				if (f.isCompleted) {
					debugLog("Starts clearing the queue")
					val newFuture = clearQueue()
					newFuture.forFailure { log(_, "Unexpected failure while clearing the queue") }
					Some(newFuture) -> newFuture
				}
				// Case: Clearance already pending => Won't do anything
				else
					None -> f
			} match {
				// Case: New clearance => Schedules another if it fails (unless stopped)
				case Some(clearanceCompletion) =>
					Detach.and(After) {
						clearanceCompletion.onComplete { result =>
							debugLog(s"Queue clearance completed with $result")
							result.logWithMessage("Unexpected failure while clearing the queue")
							if (result.toOption.forall { !_ } && stopFlag.isNotSet)
								scheduleQueueClearance()
						}
					}
				case None => Detach
			}
		// Case: Clients are not yet available => Continues listening
		else
			Continue
	}
	
	private val regeneratorWaitLock = new AnyRef
	private val regenerator = LoopingProcess.restartable.withWaitLock(regeneratorWaitLock) { hurryFlag =>
		// Case: Stopped => Exits this loop
		if (stopFlag.isSet || hurryFlag.value)
			None
		else {
			// Checks how much capacity there is for new processes
			val capacity = cleanProcesses()
			
			// Case: No capacity => Waits until the next call (or checks after 5 mins)
			if (capacity <= 0) {
				debugLog("No capacity for generating")
				Some(WaitDuration(5.minutes).breakable)
			}
			// Case: Capacity available => Starts a new instance
			else {
				// Waits until the new instance has been created
				debugLog("Generating")
				val continueDelay = startNewInstance().waitFor() match {
					case Success(process) =>
						process.completionFuture.onComplete { _ => WaitUtils.notify(regeneratorWaitLock) }
						10.seconds
					case Failure(error) =>
						log(error, "Failed to start a Vast AI instance")
						30.seconds
				}
				
				// Schedules the next loop
				// Case: More capacity is available => Continues to generate the next instance after a short delay
				if (cleanProcesses() > 0)
					Some(WaitDuration(continueDelay))
				// Case: No more capacity => Waits until one of the processes completes (checks after 5 mins anyway)
				else
					Some(WaitDuration(5.minutes).breakable)
			}
		}
	}
	
	
	// INITIAL CODE -----------------------
	
	registerToStopOnceJVMCloses()
	
	// Whenever the target instance count increases (which may be immediately), starts preparing new instances
	targetInstanceCountP.addListenerAndSimulateEvent(0) { e =>
		if (stopFlag.isSet)
			Detach
		else {
			if (e.newValue > e.oldValue) {
				WaitUtils.notify(regeneratorWaitLock)
				regenerator.runAsync()
			}
			Continue
		}
	}
	
	// Whenever the queue starts filling, schedules clearance
	hasQueueFlag.addListener { e =>
		if (e.newValue)
			scheduleQueueClearance()
		Continue
	}
	
	idleShutdownProcess.foreach { _.runAsync() }
	
	// If debug logging is enabled, starts tracking various pointers
	debugLogger.foreach { debug =>
		_queue.addContinuousListener { e => debugLogUsing(debug, s"${ e.newValue.size } requests are queued now") }
		targetInstanceCountP.addContinuousListener { e =>
			debugLogUsing(debug, s"Now targeting ${ e.newValue } instances instead of the previous ${ e.oldValue }")
		}
		processorsP.addContinuousListener { e => debugLogUsing(debug, s"Now using ${ e.newValue.size } processors") }
		usableProcessorsP.addContinuousListener { e =>
			debugLogUsing(debug, s"${ e.newValue.size } processors are now fully usable")
		}
		
		// Also prints the status regularly
		Loop.after(1.minutes) {
			debug { writer =>
				writer.println(s"${ Now.toLocalTime }: Status:")
				status.processorStates.foreach { status =>
					writer.println(s"\t- ${ status.phase.name }: ${ status.activeTokens } active + ${
						status.pendingTokens } pending")
				}
			}
			if (stopFlag.isSet && processors.isEmpty)
				None
			else
				Some(1.minutes)
		}
	}
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return The current status of this executor
	 */
	def status = VastAiVllmChatExecutorStatus(processors.map { _.status }, _queue.size)
	
	/**
	 * Maximum request (context) size that is possible to handle without using backup executors
	 */
	def maxContextSize = safeMaxContextSize - settings.contextSafetyMargin * 2 - 1
	/**
	 * @return Maximum context size used internally
	 */
	private def safeMaxContextSize = maxContextSizeP.value
	
	/**
	 * @return Whether there's at least one Vast AI instance ready to process incoming requests
	 */
	def usable = usableProcessorsP.value.exists { _.usable }
	/**
	 * @return Number of currently usable Vast AI instances / vLLM APIs
	 */
	def usableProcessorsCount = usableProcessorsP.value.count { _.usable }
	
	private def processors = processorsP.value
	
	private def debugLogger = settings.debugLogger
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(params: ChatParams): Future[RequestResult[BufferedOpenAiReply]] = {
		// Checks the required context size
		// Also adds a safety margin
		val requiredContext = params(ContextTokens).int match {
			case Some(context) => Some(context + settings.contextSafetyMargin)
			case None => settings.defaultContextSize
		}
		requiredContext match {
			case Some(tokens) =>
				// Case: Too large a request => Uses the backup executor, if available
				if (tokens > safeMaxContextSize)
					delegateToBackup(params, Response.Failure(BadRequest,
						s"Maximum context size of $safeMaxContextSize is exceeded by $tokens"))
				// Case: Suitable context size => Delegates processing to one of available clients
				else
					push(Request(params, tokens))
			
			// Case: No context size known (never recommended) => Uses the backup executor, if available
			case None =>
				delegateToBackup(params,
					RequestSendingFailed(new IllegalArgumentException("Request context size was not specified")))
		}
	}
	
	override def stop(): Future[Any] = {
		debugLog("Stop called")
		val regeneratorStopFuture = regenerator.stop()
		if (stopFlag.set()) {
			// Stops all active processors
			val processorStopFutures = processors.map { _.stop() }
			// Fails every queued request
			_queue.popAll().foreach { _.fail() }
			// Destroys all stopped instances, unless required to keep them
			val destroyFutures = {
				if (settings.preservesStoppedInstances) {
					val preservedInstanceIds = stoppedInstanceIdsP.value
					if (preservedInstanceIds.nonEmpty)
						debugLog(s"Preserving ${ preservedInstanceIds.size } previously stopped instances")
					Empty
				}
				else {
					val instanceIdsToDestroy = stoppedInstanceIdsP.popAll()
					if (instanceIdsToDestroy.nonEmpty)
						debugLog(s"Destroying ${ instanceIdsToDestroy.size } previously stopped instances")
					instanceIdsToDestroy.map { instanceId =>
						vastAiClient.send(DestroyInstance(instanceId)).map {
							case failure: RequestFailure =>
								debugLog(s"Failed to destroy instance $instanceId: ${failure.cause.getMessage}")
								log(failure.cause, "Failed to destroy a Vast AI instance",
									Model.from("instanceId" -> instanceId))
								
							case _ => ()
						}
					}
				}
			}
			scala.collection.View.concat(Single(regeneratorStopFuture), processorStopFutures, destroyFutures).future
		}
		// Case: Already requested to stop => Just yields the regenerator process stop future
		else
			regeneratorStopFuture
	}
	
	
	// OTHER    ---------------------------
	
	private def delegateToBackup(request: ChatParams, failure: => RequestFailure) =
		settings.backupExecutor match {
			case Some(backup) => backup(request)
			case None => Future.successful(failure)
		}
	
	// Notice: This request is pretty deeply recursive, being called from Processor
	private def push(request: Request): Future[RequestResult[BufferedOpenAiReply]] = {
		// Case: Already stopped => Fails
		if (stopFlag.isSet)
			Future.successful(RequestSendingFailed(
				new IllegalStateException("This request-execution interface was closed")))
		// Case: Request was deprecated => Resolves it immediately
		else if (request.params.deprecated)
			Future.successful(RequestWasDeprecated)
		else {
			// Determines the processors to target
			// In situations where most processors are loading,
			// limits the accepted request volume and queues requests instead
			// (so that we won't end up with 1M tokens in one processor and 10K in another later)
			val processors = this.processors
			val (unusableProcessors, usableProcessors) = processors.divideBy { _.usable }.toTuple
			val targetProcessors = {
				if (usableProcessors.isEmpty || usableProcessors.hasSize > unusableProcessors)
					usableProcessors
				else
					usableProcessors.filter {
						_.pendingTokens <= settings.instanceAccelerationPendingTokensThreshold.value
					}
			}
			
			// Attempts to give the request to one of the processors, preferring those least used
			targetProcessors.sortBy { _.pendingToActiveRatio }
				.findMap { processor =>
					processor.tryPush(request).map { resultFuture =>
						// Activates more instances, if appropriate
						adjustInstanceTargetIfAppropriate(pendingTokensCheckThresholdP, request.tokens.value,
							settings.instanceActivationPendingTokensThreshold.value, 5000,
							current = {
								// Counts the threshold based on the lowest used processor
								val usableProcessors = processors.filter { _.usable }
								usableProcessors.iterator.map { _.pendingTokens }.minOption match {
									case Some(lowestUse) => lowestUse.toInt * usableProcessors.size
									case None => 0
								}
							}) {
							currentTarget =>
								if (processors.size < currentTarget || processors.exists { _.phase < Serving })
									currentTarget * settings.instanceAccelerationPendingTokensThreshold.value
								else
									currentTarget * settings.instanceActivationPendingTokensThreshold.value
						}
						
						// Checks whether it's possible to handle cases where the context window couldn't
						// fit into the server's maximum context (400 error, because of token-counting issues)
						processor.usableContextSize.flatMap { currentContextSize =>
							// Attempts to reserve at least 20% more space
							val nextContextSize = (currentContextSize * 1.2) min safeMaxContextSize
							if (processors.exists { _.usableContextSize.exists { _ >= nextContextSize } })
								Some(nextContextSize)
							else
								None
						} match {
							// Case: A processor with a larger max context size is available
							//       => Applies backup logic for 400 context size issues
							case Some(nextContextSize) =>
								resultFuture.flatMap {
									case failure: Response.Failure if failure.status == BadRequest =>
										debugLog(s"${ failure.message } => Delegates the request to a processor with a larger context window")
										log(s"Warning: ${ failure.message } => Delegated the request to a processor with a larger context window")
										request.asRetryWithIfPossible(failure.cause) match {
											case Some(retry) => push(retry.withTokens(nextContextSize))
											case None => Future.successful(failure)
										}
									case result => Future.successful(result)
								}
							// Case: No backup processor is available => Won't add recovery processes
							case None => resultFuture
						}
					}
				}
				// Case: No processors are available => Queues this request
				.getOrElse { queueRequest(request) }
		}
	}
	/**
	 * Actually executes a request using a specific Vast AI instance.
	 * Called from individual processors.
	 * @param request Request to process
	 * @param client Client that should handle this request
	 * @param model Model hosted by the utilized instance
	 * @param maxContextSize Maximum context size of the utilized instance
	 * @return Future of the eventual request result
	 */
	private def push(request: Request, client: RequestQueue, model: String,
	                 maxContextSize: TokenCount): Future[RequestResult[BufferedOpenAiReply]] =
	{
		// Converts the request into a full chat request
		val apiRequest = BufferedOpenAiChatCompletionRequest(
			request.params.toLlm(llmCache(model)).mapSetting(ContextTokens) { _.int match {
				case Some(maxTokens) => maxTokens min maxContextSize.value
				case None => maxContextSize.value
			} })
		
		// Adds handling for situations where instance-closing leads to request deprecation
		// Causes such requests to be attempted again
		client.push(apiRequest).future.flatMap {
			// Case: Request was marked as deprecated at a lower process level => Attempts that request again
			case RequestWasDeprecated if !apiRequest.deprecated && stopFlag.isNotSet =>
				request.asRetryIfPossible match {
					case Some(retry) => push(retry)
					// Case: Can't retry anymore => Returns a failure or a deprecation, depending on earlier results
					case None =>
						request.firstError match {
							case Some(error) => Future.successful(RequestSendingFailed(error))
							case None => Future.successful(RequestWasDeprecated)
						}
				}
			// Case: Request completed or terminated for another reason, or this system stopped => Finishes
			case result => Future.successful(result)
		}
	}
	
	private def queueRequest(request: Request) = {
		val task = new QueuedTask(request)
		val queueSize = _queue.updateAndGet { _ :+ task }.size
		
		// Activates more instances, if appropriate
		adjustInstanceTargetIfAppropriate(queueSizeCheckThresholdP, 1, settings.instanceActivationQueueSizeThreshold,
			5, queueSize) { _ * settings.instanceActivationQueueSizeThreshold }
		
		task.future
	}
	
	/**
	 * Adjusts [[targetInstanceCountP]], if that is deemed appropriate
	 * @param checkThresholdP A pointer that's used for limiting these checks
	 * @param adjustment Adjustment that should be applied to 'checkThresholdP'
	 * @param thresholdIncrement Increase to 'checkThresholdP' after a check has been applied (call-by-name)
	 * @param thresholdAtMaxInstances Next 'checkThresholdP' value applied in case we're already at max instances
	 *                                (call-by-name)
	 * @param current Current instance utilization value (either pending tokens or queue size, depending on the context).
	 *                Call-by-name.
	 * @param thresholdFrom A function for calculating the next target instance increase threshold.
	 *                      Receives the current target instance count.
	 */
	private def adjustInstanceTargetIfAppropriate(checkThresholdP: Pointer[Int], adjustment: Int,
	                                              thresholdIncrement: => Int, thresholdAtMaxInstances: => Int,
	                                              current: => Int)
	                                             (thresholdFrom: Int => Int) =
		if (checkThresholdP.updateAndGet { _ - adjustment } <= 0) {
			// Checks whether new instances should be generated
			checkThresholdP.value = targetInstanceCountP.mutate { currentTarget =>
				// Case: Already at max instances => Keeps the current target
				if (currentTarget >= settings.maxInstanceCount)
					thresholdAtMaxInstances -> currentTarget
				// Case: Below the core instance level => Activates the core instances
				else if (currentTarget < settings.coreInstanceCount)
					thresholdIncrement -> settings.coreInstanceCount
				else {
					// If there are instances which are getting ready, uses a different activation threshold
					val threshold = thresholdFrom(currentTarget)
					val overThreshold = current - threshold
					
					// Case: Enough for activation => Adjusts the target instance count
					if (overThreshold >= 0)
						(thresholdIncrement - overThreshold, currentTarget + 1)
					// Case: Not enough => Keeps the current target
					else
						-overThreshold -> currentTarget
				}
			}
		}
	
	/**
	 * Schedules the next queue-clearance, which is initiated when at least one processor becomes usable.
	 */
	private def scheduleQueueClearance(): Unit =
		usableProcessorsP.addListenerAndSimulateEvent(Empty)(clearQueueListener)
	
	/**
	 * A recursive process for clearing the request queue
	 * @return A future that resolves once either:
	 *              - No processors are available
	 *              - The queue becomes empty
	 *
	 *         Yields whether the queue is now empty.
	 */
	private def clearQueue(): Future[Boolean] = {
		// Checks the usable processors
		val processors = {
			val usable = this.processors.filter { _.usable }
			val usableCount = usable.size
			// If there are multiple processors, selects those least used
			val selected = {
				if (usableCount <= 1)
					usable
				else {
					val targetProcessors = (usableCount - 1) min 3
					val sorted = usable.sortBy { _.pendingTokens }
					// Selects all processors that have no pending tasks
					val free = sorted.takeWhile { _.pendingTokens <= 0 }
					val freeCount = free.size
					// Always selects at least X processors
					if (freeCount >= targetProcessors)
						free
					else
						free ++ sorted.slice(freeCount, targetProcessors)
				}
			}
			selected.sortBy { _.maxContextSize.getOrElse(TokenCount.zero) }
		}
		processors.lastOption.flatMap { _.maxContextSize } match {
			case Some(maxProcessorSize) =>
				// Collects the tasks to process
				// If there are tasks that are too large for the current processors, delays them
				val tasks = _queue.mutate { queue =>
					val iter = queue.iterator
					val keepBuilder = OptimizedIndexedSeq.newBuilder[QueuedTask]
					val processBuilder = OptimizedIndexedSeq.newBuilder[QueuedTask]
					var remainingCapacity = processors.size
					
					while (remainingCapacity > 0 && iter.hasNext) {
						val next = iter.next()
						if (next.tokens > maxProcessorSize)
							keepBuilder += next
						else {
							processBuilder += next
							remainingCapacity -= 1
						}
					}
					
					processBuilder.result().sortBy { _.tokens } -> (iter ++ keepBuilder.result()).toOptimizedSeq
				}
				// Case: No tasks to process => Completes
				if (tasks.isEmpty) {
					// If there were too large tasks, fails them
					_queue.popAll().foreach { _.fail() }
					Future.successful(true)
				}
				else {
					// Assigns the tasks to the available processors
					debugLog(s"Resolves the next ${ tasks.size } queued requests")
					tasks.iterator.zipWithIndex.foreach { case (task, i) =>
						processors.view.drop(i).find { _.maxContextSize.exists { _ >= task.tokens } } match {
							case Some(processor) => task.resolveUsing(processor)
							case None => task.fail()
						}
					}
					
					// After a short delay, continues to unqueue more tasks
					// Applies a longer delay, if all processors are busy
					val delay = {
						if (processors.exists { p => p.maxContextSize.exists { p.pendingTokens < _.value } })
							1.seconds
						else
							10.seconds
					}
					Delay.future(delay) { clearQueue() }
				}
				
			// Case: No clients are usable => Completes
			case None => Future.successful(_queue.isEmpty)
		}
	}
	
	/**
	 * Starts a new Vast AI instance, adding it to the processor pool.
	 * Also handles automatic shutdown & removal if the instance becomes unstable, etc.
	 * @return A future that resolves when the Vast AI instance is available (although not usable),
	 *         or if failed to acquire said instance.
	 */
	private def startNewInstance() = createInstanceAccess { _ =>
		debugLog("Starting a new instance")
		// Creates and starts the process of setting up vLLM on Vast AI
		val startTime = Now.toInstant
		val process = VastAiVllmProcess.withSettings(vllmSettings).apply(selectOffer, modelSize.modelSize) {
			// Choose image
			offer =>
				// Delegates image-choosing to the custom 'chooseImage' function
				val maxContextSize = contextSizeOn(offer.gpu.ram)
				val (image, initialVllmState, model) = chooseImage(offer, maxContextSize)
				(image, initialVllmState, maxContextSize, model)
		}
		processorsP :+= new Processor(process)
		debugLog(s"Now at ${ processors.size } instance processes")
		process.runAsync()
		
		// Updates the usableClients when API hosting starts or ends
		process.usableClientPointer.addListener { e =>
			// Checks for client-usability
			val clientStates = e.values.map { _.map { case (client, model, _) => client -> model } }
			if (clientStates.isAsymmetric) {
				debugLog("Updating client count")
				usableProcessorsP.update()
			}
		}
		process.detailedStatePointer.addListenerAndSimulateEvent(NotStarted) { e =>
			// Checks whether the instance became available => Updates max context, if so
			if (e.values.isAsymmetricBy { _.isInstanceAvailable })
				maxContextSizeP.update()
			
			// Case: Stop process initiated => Updates max context size & stops listening
			if (e.newValue.phase >= Stopping) {
				maxContextSizeP.update()
				debugLog("Stops listening to the Vast AI process")
				Detach
			}
			else
				Continue
		}
		
		// Informs the recorder once this process finishes, and when/if an API is successfully hosted
		settings.recorder.foreach { recorder =>
			process.detailedStatePointer.addListenerAndSimulateEvent(NotStarted) { e =>
				e.newValue match {
					case Hosting(instance) =>
						recorder.onApiSetup(instance, process.startTime, process.loadCompletionTime.getOrElse(Now))
						Detach
					case state if state.phase > Serving => Detach
					case _ => Continue
				}
			}
			process.recordFuture.foreach(recorder.onProcessCompleted)
		}
		
		// The next instance may be acquired once the process is in loading state / instance has been acquired
		process.detailedStatePointer
			.futureWhere { state => state.isInstanceAvailable || state.phase >= Stopping }
			.flatMap { state =>
				if (state.isInstanceAvailable) {
					consecutiveGetInstanceFailuresP.value = 0
					Future.successful(process)
				}
				// Case: Failed to acquire an instance
				//       => Waits for a while before attempting to get the next instance
				else {
					log("Warning: Failed to acquire an instance")
					// Checks whether there were too many instance-acquisition failures
					val consecutiveFailures = consecutiveGetInstanceFailuresP.updateAndGet { _ + 1 }
					// Case: Too many failures => Stops this whole system
					if (consecutiveFailures > 50) {
						debugLog("Too many failures to acquire an instance => Stops the whole system")
						stop()
						Future.successful(process)
					}
					else
						Delay(10.seconds) { process }
				}
			}
	}
	
	/**
	 * Removes completed processes from the pool
	 * @return Currently remaining capacity (i.e. how many more processes may be started)
	 */
	private def cleanProcesses() = processorsP.mutate { processes =>
		val remaining = processes.filterNot { _.terminated }
		debugLogger.foreach { log =>
			val removed = processes.filterNot(remaining.contains)
			if (removed.nonEmpty) {
				log { writer =>
					writer.println(s"${ Now.toLocalTime }: Removed ${ removed.size } completed Vast AI processes:")
					removed.foreach { p => writer.println(s"\t- ${ p.instanceId.mkString }: ${ p.vastAiState }") }
				}
			}
		}
		(targetInstanceCountP.value - remaining.size) -> remaining
	}
	
	/**
	 * Calculates the maximum context size to apply on different devices
	 * @param vram Amount of VRAM on the machine
	 * @return Maximum context size to apply
	 */
	private def contextSizeOn(vram: ByteCount) =
		modelSize.maxContextSizeOn(vram * vllmSettings.maxGpuUtil) - settings.contextSafetyMargin
		
	private def debugLog(entry: => String) = debugLogger.foreach { debugLogUsing(_, entry) }
	private def debugLogUsing(logger: KeptOpenWriter, entry: String) =
		logger { _.println(s"${ Now.toLocalTime }: $entry") }
	
	
	// NESTED   ---------------------------
	
	private object ReuseStoppedInstances extends InstanceReuseLogic
	{
		override def reuses: Boolean = stoppedInstanceIdsP.nonEmpty
		
		override def findReusableFrom(instances: Seq[VastAiInstance]): Option[VastAiInstance] = {
			// Finds a previously stopped instance
			val instanceToReuse = stoppedInstanceIdsP.value.findMap { id => instances.find { _.id == id } }
			// If found, reuses it and removes it from the list of stopped instances
			instanceToReuse.foreach { instance => stoppedInstanceIdsP.filterNotCurrent { _ == instance.id } }
			instanceToReuse
		}
	}
	
	/**
	 * Manages / utilizes an individual Vast AI process for request-handling
	 * @param process The utilized Vast AI process
	 */
	private class Processor(process: VastAiVllmProcess) extends Breakable with MaybeEmpty[Processor]
	{
		// ATTRIBUTES   ------------------
		
		private val started = Now.toInstant
		private var stopped = false
		private var _lastRequestTime = started
		private var _lastPendingStartTime = started
		private var _lastPendingEndTime = started
		
		// Once / if a vLLM server becomes available, creates a funnel to wrap it
		private val funnelP = process.clientPointer.map { _.map { _.map { case (queue, model, maxContextSize) =>
			// Updates the last request -time now that the API is available.
			// Also, makes sure the idle-clearing process is running.
			_lastRequestTime = Now
			_lastPendingStartTime = _lastRequestTime
			_lastPendingEndTime = _lastRequestTime
			idleShutdownProcess.foreach { _.runAsync() }
			
			// Prepares a funnel for the incoming requests
			val safeMaxContextSize = maxContextSize - settings.contextSafetyMargin
			debugLog(s"Starting a funnel of $safeMaxContextSize tokens")
			val funnel = MappingFunnel(safeMaxContextSize.value) {
				request: Request => request.tokens.value } {
				request =>
					if (process.detailedState.isUsable) {
						_lastRequestTime = Now
						push(request, queue, model.name, safeMaxContextSize)
					}
					else
						request.asRetryWith(
							new IllegalStateException(s"Unusable process state: ${ process.detailedState }")) match
						{
							case Success(retry) => push(retry)
							case Failure(error) => Future.successful(RequestSendingFailed(error))
						}
			}
			
			// Takes notice when the funnel becomes full or only partially filled
			funnel.pendingFlag.addListener { e =>
				if (e.newValue)
					_lastPendingStartTime = Now
				else
					_lastPendingEndTime = Now
				
				if (stopped) Detach else Continue
			}
			
			funnel -> safeMaxContextSize
		} } }
		
		
		// COMPUTED ---------------------
		
		def instanceId = process.instanceId
		
		def phase = process.detailedState.phase
		def vastAiState = process.vastAiState
		def instanceStatus = process.instanceStatus
		
		/**
		 * @return Whether this processor is capable of handling (more) requests at this time
		 */
		def usable = !stopped && process.detailedState.isUsable && funnelP.value.exists { _.isSuccess }
		def terminated = process.state.isFinal
		
		def maxContextSize = process.maxContextSize
		def usableContextSize: Option[TokenCount] = {
			if (stopped || process.detailedState.isUnusable)
				None
			else
				funnel.map { _._2 }
		}
		
		def pendingTokens = funnel match {
			case Some((funnel, _)) => funnel.queuedCapacity
			case None => 0.0
		}
		def pendingToActiveRatio = funnel match {
			case Some((funnel, maxContextSize)) => funnel.queuedCapacity / maxContextSize.value
			case None => 1000000
		}
		
		def notFullyUtilized = funnel.exists { !_._1.containsPendingActions }
		
		def wasRequestedToStop = stopped
		def lastRequestTime = _lastRequestTime
		def lastPendingEndTime = _lastPendingEndTime
		
		def status = {
			val (active, pending) = funnel match {
				case Some((funnel, _)) => funnel.activeUtilization.toInt -> funnel.queuedCapacity.toInt
				case None => 0 -> 0
			}
			VastAiVllmProcessorStatus(phase, instanceStatus, active, pending,
				maxContextSize.getOrElse(defaultMaxContextSize), started, _lastRequestTime, _lastPendingStartTime,
				_lastPendingEndTime)
		}
		
		/**
		 * @return The currently available funnel, if applicable.
		 *         None if no funnel is available at this time.
		 */
		private def funnel: Option[(MappingFunnel[Request, RequestResult[BufferedOpenAiReply]], TokenCount)] =
			funnelP.value.flatMap { _.toOption }
		
		
		// IMPLEMENTED  ------------------
		
		override def self: Processor = this
		
		override def isEmpty: Boolean = funnel.forall { _._1.isEmpty }
		
		override def stop(): Future[Any] = {
			stopped = true
			val state = process.detailedState
			// Case: Not currently hosting an API => Requests the Vast AI process to stop
			if (state.phase != Serving)
				process.stop()
			// Case: Hosting an API
			//       => Makes sure the queued requests get resolved first, before requesting the instance to stop
			else
				funnel match {
					case Some((funnel, _)) => funnel.emptyFuture.flatMap { _ => process.stop() }
					case None => process.stop()
				}
		}
		
		
		// OTHER    ----------------------
		
		/**
		 * Attempts to give a request for this processor to handle
		 * @param request Request to process
		 * @return Request result future. None if this processor can't handle that request at this time.
		 */
		def tryPush(request: Request) =
			funnel
				// Makes sure the funnel can handle this request
				.filter { case (_, maxContextSize) =>
					!stopped && request.tokens <= maxContextSize && process.detailedState.isUsable }
				.map { _._1.push(request) }
	}
	
	private class QueuedTask(request: Request)
	{
		// ATTRIBUTES   ---------------------------
		
		private val promise = Promise[Option[Processor]]()
		val future: Future[RequestResult[BufferedOpenAiReply]] = promise.future.flatMap {
			// Case: A processor became available => Gives it this request
			case Some(processor) =>
				processor.tryPush(request).getOrElse {
					log(s"The prepared processor couldn't receive the queued request of $tokens tokens")
					
					// Case: The processor couldn't receive this request (unexpected)
					//       => Attempts again with another processor (recursive)
					if (stopFlag.isSet)
						Future.successful(RequestSendingFailed(
							new IllegalStateException("This interface was stopped before this request could be handled")))
					else
						request.asRetryWith(
							new IllegalStateException("The activated processor rejected this request")) match
						{
							case Success(retry) => push(retry)
							case Failure(error) => Future.successful(RequestSendingFailed(error))
						}
				}
			// Case: No processor could handle this request => Fails
			case None =>
				val failure = {
					if (stopFlag.isSet)
						new IllegalStateException("This interface was stopped before this request could be handled")
					else
						new IllegalArgumentException(
							s"No processor was able to receive this request of $tokens tokens")
				}
				Future.successful(RequestSendingFailed(failure))
		}
		
		
		// COMPUTED ------------------------------
		
		def tokens = request.tokens
		
		
		// OTHER    ------------------------------
		
		def resolveUsing(processor: Processor) = promise.success(Some(processor))
		def fail() = promise.success(None)
	}
	
	// TODO: Possibly chain the errors
	private case class Request(params: ChatParams, tokens: TokenCount, pastRetries: Int = 0,
	                           firstError: Option[Throwable] = None)
	{
		// COMPUTED ----------------------
		
		/**
		 * @return Whether this request may still be retried
		 */
		def mayBeRetried = pastRetries < maxRetries
		
		/**
		 * Creates a copy of this request, representing another attempt
		 * @return A copy of this request, marked as a retry. Failure if too many failures were encountered.
		 */
		def asRetry = {
			if (pastRetries >= maxRetries)
				Failure(firstError.getOrElse { new IllegalStateException("Too many retries") })
			else
				Success(copy(pastRetries = pastRetries + 1))
		}
		/**
		 * @return If this request may still be retried, returns a copy marked as a retry
		 */
		def asRetryIfPossible = if (mayBeRetried) Some(copy(pastRetries = pastRetries + 1)) else None
		
		
		// OTHER    ----------------------
		
		/**
		 * Creates a copy of this request, representing another attempt
		 * @param error Error to fail with, if maximum number of retries was encountered (call-by-name)
		 * @return A copy of this request, marked as a retry. Failure if too many failures were encountered.
		 */
		def asRetryWith(error: => Throwable) = {
			if (mayBeRetried)
				Success(copy(pastRetries = pastRetries + 1, firstError = firstError.orElse(Some(error))))
			else
				Failure(firstError.getOrElse(error))
		}
		/**
		 * @param error Error to record as the first encountered error, if applicable (call-by-name)
		 * @return If this request may still be retried, returns a copy marked as a retry
		 */
		def asRetryWithIfPossible(error: => Throwable) = {
			if (mayBeRetried)
				Some(copy(pastRetries = pastRetries + 1, firstError = firstError.orElse(Some(error))))
			else
				None
		}
		
		def withTokens(tokens: TokenCount) = copy(tokens = tokens)
	}
}
