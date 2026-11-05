package utopia.echo.model.vastai.settings

import utopia.echo.controller.chat.BufferingChatRequestExecutor
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.vastai.process.VastAiVllmProcessRecorder
import utopia.echo.model.vastai.settings.VastAiVllmChatExecutorSettings.VastAiVllmChatExecutorSettingsFactory
import utopia.echo.model.vastai.settings.VastAiVllmSettings.{VastAiVllmSettingsFactory, VastAiVllmSettingsWrapper}
import utopia.flow.async.context.Scheduler
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Today}
import utopia.flow.util.Mutate
import utopia.flow.util.logging.{Logger, SysErrLogger}

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object VastAiVllmChatExecutorSettings
{
	// ATTRIBUTES   --------------------------
	
	lazy val default = apply()
	
	
	// IMPLICIT ------------------------------
	
	implicit def asVllmSettings(settings: VastAiVllmChatExecutorSettings): VastAiVllmSettings =
		settings.vllmSettings
	implicit def asServiceSettings(settings: VastAiVllmChatExecutorSettings): VastAiServiceSettings =
		settings.vllmSettings.serviceSettings
		
	
	// NESTED   ------------------------------
	
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
		 *                       None if no backup executor should be used.
		 * @return A copy of this instance using the specified backup processing logic.
		 */
		def usingBackupExecutor(backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]]): Repr
		
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
		 * @param backupExecutor A request processing logic to use for requests that are too large
		 *                       for this executor to process. I.e. for requests that require a context size larger
		 *                       than any of the rented Vast AI instances can process.
		 * @return A copy of this instance using the specified backup processing logic.
		 */
		def usingBackupExecutor(backupExecutor: BufferingChatRequestExecutor[BufferedOpenAiReply]): Repr =
			usingBackupExecutor(Some(backupExecutor))
		
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
		override def usingBackupExecutor(backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]]): Repr =
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
 * @author Mikko Hilpinen
 * @since 05.11.2026, v1.6
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
	
	override def usingBackupExecutor(backupExecutor: Option[BufferingChatRequestExecutor[BufferedOpenAiReply]]): VastAiVllmChatExecutorSettings =
		copy(backupExecutor = backupExecutor)
	
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