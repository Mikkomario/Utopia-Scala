package utopia.echo.controller.vastai.vllm

import utopia.annex.controller.LockingRequestQueue
import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.echo.controller.client.{LlmServiceClient, VastAiApiClient}
import utopia.echo.controller.vastai.VastAiServiceProcess.{VastAiServiceSettings, VastAiServiceSettingsFactory, VastAiServiceSettingsWrapper}
import utopia.echo.controller.vastai.vllm.VastAiVllmProcess.VastAiVllmSettings
import utopia.echo.controller.vastai.{SelectOffer, SshExecutor, VastAiServiceProcess}
import utopia.echo.model.enumeration.ServiceState
import utopia.echo.model.request.openai.ListOpenAiModels
import utopia.echo.model.response.openai.OpenAiModelInfo
import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.instance.NewInstanceFoundation
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.flow.async.context.Scheduler
import utopia.flow.async.process.LoopingProcess
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Settable
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object VastAiVllmProcess
{
	// ATTRIBUTES   -------------------
	
	val factory = VastAiVllmProcessFactory(VastAiVllmSettings.default)
	
	
	// COMPUTED -----------------------
	
	private def defaultGateway = Gateway(maxConnectionsPerRoute = 4, maxConnectionsTotal = 4,
		maximumTimeout = Timeout(read = 10.minutes, manager = 15.minutes), connectionTimeout = 60.seconds,
		disableTrustStoreVerification = true)
	
	
	// IMPLICIT -----------------------
	
	// Implicitly treats this object as a factory
	implicit def objectAsFactory(o: VastAiVllmProcess.type): VastAiVllmProcessFactory = o.factory
			
	
	// NESTED   ---------------------------
	
	trait VastAiVllmSettingsFactory[+Repr] extends VastAiServiceSettingsFactory[Repr]
	{
		// ABSTRACT -----------------------
		
		/**
		 * @param additionalSpace Additional disk space to reserve besides the space required for the LLM.
		 * @return Copy of this instance reserving the specified amount of additional disk space.
		 */
		def reservingAdditionalDiskSpace(additionalSpace: ByteCount): Repr
		/**
		 * @param lazyGateway A lazily initialized Gateway instance
		 * @return A copy of this instance using the specified Gateway interface
		 */
		def usingLazyGateway(lazyGateway: View[Gateway]): Repr
		/**
		 * @param maxUtilRatio Maximum GPU utilization ratio (up to 1.0)
		 * @return A copy of this instance utilizing the GPU up to that limit
		 */
		def utilizingGpuUpTo(maxUtilRatio: Double): Repr
		/**
		 * Applies a limit to parallel requests
		 * @param maxParallelRequests Maximum number of requests to run in parallel.
		 * @return Copy of this instance with the specified parallel requests -limit
		 */
		def withMaxParallelRequests(maxParallelRequests: Int): Repr
		/**
		 * @param extraStartupArgs Additional arguments passed to vLLM serve
		 * @return A copy of this instance applying the specified startup arguments
		 */
		def applyingExtraStartupArgs(extraStartupArgs: String): Repr
		/**
		 * @param timeout Timeout for started API requests.
		 *                If this timeout is reached, the request queue is closed
		 *                and the underlying Vast AI instance is destroyed.
		 * @return A copy of this instance using the specified response timeout
		 */
		def withResponseTimeout(timeout: Duration): Repr
		
		
		// COMPUTED -----------------------
		
		/**
		 * @param gateway A Gateway instance to use (call-by-name)
		 * @return A copy of this instance using the specified Gateway interface
		 */
		def usingLazyGateway(gateway: => Gateway) = usingLazyGateway(Lazy(gateway))
		/**
		 * @param gateway A Gateway instance to use
		 * @return A copy of this instance using the specified Gateway interface
		 */
		def usingGateway(gateway: Gateway) = usingLazyGateway(View.fixed(gateway))
	}
	
	object VastAiVllmSettings
	{
		// ATTRIBUTES   ---------------------
		
		/**
		 * Default settings
		 */
		lazy val default = VastAiVllmSettings()
		
		
		// IMPLICIT -------------------------
		
		// Implicitly accesses VastAIServiceSettings properties
		implicit def asServiceSettings(vllmSettings: VastAiVllmSettings): VastAiServiceSettings =
			vllmSettings.serviceSettings
	}
	/**
	 * @param additionalReservedDisk Additional disk space to reserve, beyond the model size. Default = 5 GB.
	 * @param lazyGateway A lazily initialized [[Gateway]] instance to use for connecting to the rented instance.
	 *                    Default = new Gateway with 4 max connections.
	 *                    Note: The used Gateway instance determines the generated request queue's width.
	 * @param maxGpuUtil Maximum GPU utilization, as a fraction between 0 and 1. Used when/if starting vLLM.
	 *                   Default = 0.9.
	 * @param maxParallelRequests Maximum number of requests to run in parallel.
	 *                            None (default), if parallelism should not be limited on this level.
	 *
	 *                            Note: The used Gateway instance may also limit the number of parallel HTTP connections.
	 * @param extraStartupArgs Additional arguments passed to vLLM serve (default = empty)
	 * @param noResponseTimeout Timeout for started API requests.
	 *                          If this timeout is reached, the request queue is closed
	 *                          and the underlying Vast AI instance is destroyed.
	 *                          Default = 10 minutes.
	 * @param serviceSettings Settings to apply on the service level.
	 *                        Default = Use default service settings, but activate port forwarding from & to ports 8000.
	 */
	case class VastAiVllmSettings(additionalReservedDisk: ByteCount = 5.gb,
	                              lazyGateway: View[Gateway] = Lazy(defaultGateway), maxGpuUtil: Double = 0.9,
	                              maxParallelRequests: Option[Int] = None,
	                              extraStartupArgs: String = "", noResponseTimeout: Duration = 10.minutes,
	                              serviceSettings: VastAiServiceSettings = VastAiServiceSettings.default.withPortForwarding(8000, 8000))
		extends VastAiVllmSettingsFactory[VastAiVllmSettings] with VastAiServiceSettingsWrapper[VastAiVllmSettings]
	{
		// COMPUTED ---------------------------
		
		/**
		 * @return Gateway instance to use
		 */
		def gateway = lazyGateway.value
		
		
		// IMPLEMENTED  -----------------------
		
		override def reservingAdditionalDiskSpace(additionalSpace: ByteCount): VastAiVllmSettings =
			copy(additionalReservedDisk = additionalSpace)
		override def usingLazyGateway(lazyGateway: View[Gateway]): VastAiVllmSettings = copy(lazyGateway = lazyGateway)
		override def utilizingGpuUpTo(maxUtilRatio: Double): VastAiVllmSettings = copy(maxGpuUtil = maxUtilRatio)
		override def withMaxParallelRequests(maxParallelRequests: Int): VastAiVllmSettings =
			copy(maxParallelRequests = Some(maxParallelRequests))
		override def applyingExtraStartupArgs(extraStartupArgs: String): VastAiVllmSettings =
			copy(extraStartupArgs = extraStartupArgs)
		override def withResponseTimeout(timeout: Duration): VastAiVllmSettings = copy(noResponseTimeout = timeout)
		override def withSettings(settings: VastAiServiceSettings): VastAiVllmSettings =
			copy(serviceSettings = settings)
	}
	
	trait VastAiVllmSettingsWrapper[+Repr]
		extends VastAiVllmSettingsFactory[Repr] with VastAiServiceSettingsWrapper[Repr]
	{
		// ABSTRACT -----------------------
		
		/**
		 * @return Vast AI vLLM settings to apply
		 */
		def vllmSettings: VastAiVllmSettings
		/**
		 * @param settings New settings to apply
		 * @return A copy of this instance applying the specified settings
		 */
		def withSettings(settings: VastAiVllmSettings): Repr
		
		
		// IMPLEMENTED  -------------------
		
		override def serviceSettings: VastAiServiceSettings = vllmSettings.serviceSettings
		
		override def reservingAdditionalDiskSpace(additionalSpace: ByteCount): Repr =
			mapVllmSettings { _.reservingAdditionalDiskSpace(additionalSpace) }
		override def usingLazyGateway(lazyGateway: View[Gateway]): Repr =
			mapVllmSettings { _.usingLazyGateway(lazyGateway) }
		override def utilizingGpuUpTo(maxUtilRatio: Double): Repr = mapVllmSettings { _.utilizingGpuUpTo(maxUtilRatio) }
		override def withMaxParallelRequests(maxParallelRequests: Int): Repr =
			mapVllmSettings { _.withMaxParallelRequests(maxParallelRequests) }
		override def applyingExtraStartupArgs(extraStartupArgs: String): Repr =
			mapVllmSettings { _.applyingExtraStartupArgs(extraStartupArgs) }
		override def withResponseTimeout(timeout: Duration): Repr = mapVllmSettings { _.withResponseTimeout(timeout) }
		override def withSettings(settings: VastAiServiceSettings): Repr = mapVllmSettings { _.withSettings(settings) }
		
		
		// OTHER    -------------------------
		
		def mapVllmSettings(f: Mutate[VastAiVllmSettings]) = withSettings(f(vllmSettings))
	}
	
	case class VastAiVllmProcessFactory(vllmSettings: VastAiVllmSettings)
		extends VastAiVllmSettingsWrapper[VastAiVllmProcessFactory]
	{
		// IMPLEMENTED  ----------------------
		
		override def withSettings(settings: VastAiVllmSettings): VastAiVllmProcessFactory =
			copy(vllmSettings = settings)
			
		
		// OTHER    -------------------------
		
		/**
		 *
		 * @param selectOffer Logic for selecting a Vast AI instance offer
		 * @param modelSize Size of the used model. Used for calculating the reserved disk space.
		 *                  If various model sizes are used, specify the largest.
		 * @param chooseImage A function for choosing the image or Vast AI template to use.
		 *                    Accepts the selected offer, yields:
		 *                          1. Instance-creation settings
		 *                          1. Expected initial vLLM service state at the remote instance
		 *                          1. Maximum context size applied or applicable
		 *                          1. Name of the model to start vLLM with.
		 *                             Optional if vLLM is started automatically by the image / template.
		 * @param exc Implicit execution context to use
		 * @param scheduler Implicit scheduler used
		 * @param log Implicit logging interface used
		 * @param vastAiClient Implicit Vast AI client interface to use
		 * @return A new Vast AI + vLLM process (not started)
		 */
		def apply(selectOffer: SelectOffer, modelSize: ByteCount)
		         (chooseImage: Offer => (NewInstanceFoundation, ServiceState, TokenCount, String))
		         (implicit exc: ExecutionContext, scheduler: Scheduler, log: Logger, vastAiClient: VastAiApiClient) =
			new VastAiVllmProcess(selectOffer, modelSize, vllmSettings)(chooseImage)
	}
}

/**
 * A process for setting up and managing a vLLM server on a rented Vast AI instance
 * @param selectOffer Logic for selecting a Vast AI instance offer
 * @param modelSize Size of the used model. Used for calculating the reserved disk space.
 *                  If various model sizes are used, specify the largest.
 * @param settings Settings to apply. Default = use default settings.
 * @param _chooseImage A function for choosing the image or Vast AI template to use.
 *                    Accepts the selected offer, yields:
 *                          1. Instance-creation settings
 *                          1. Expected initial vLLM service state at the remote instance
 *                          1. Maximum context size applied or applicable
 *                          1. Name of the model to start vLLM with.
 *                             Optional if vLLM is started automatically by the image / template.
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
class VastAiVllmProcess(selectOffer: SelectOffer, modelSize: ByteCount,
                        settings: VastAiVllmSettings = VastAiVllmSettings.default)
                       (_chooseImage: Offer => (NewInstanceFoundation, ServiceState, TokenCount, String))
                       (implicit exc: ExecutionContext, scheduler: Scheduler, log: Logger,
                        vastAiClient: VastAiApiClient)
	extends VastAiServiceProcess[LlmServiceClient, (LockingRequestQueue, OpenAiModelInfo, TokenCount), OpenAiModelInfo](
		selectOffer, modelSize + settings.additionalReservedDisk, settings.serviceSettings)
{
	// ATTRIBUTES   ------------------------
	
	// Only requests the port once it is actually needed
	private lazy val (localPort, remotePort) = settings.serviceSettings.forwardedPorts match {
		case Some(ports) => ports.value.toTuple
		case None => (8000, 8000)
	}
	
	/**
	 * Name of the model to serve. Specified when creating the instance.
	 */
	private var modelToServe: String = ""
	/**
	 * Applied maximum context size. Specified when creating the instance.
	 */
	private var _maxContextSize: TokenCount = TokenCount.zero
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return Maximum context size applied on the model.
	 *         None if maximum context size hasn't been determined (i.e. if no offer has been accepted yet).
	 */
	def maxContextSize = _maxContextSize.ifPositive
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def chooseImage(acceptedOffer: Offer): (NewInstanceFoundation, ServiceState) = {
		val (base, vllmDefaultState, contextSize, model) = _chooseImage(acceptedOffer)
		modelToServe = model
		_maxContextSize = contextSize
		
		base -> vllmDefaultState
	}
	
	override protected def initializeService(ssh: SshExecutor): (LlmServiceClient, String) = {
		val internalClient = new LlmServiceClient(settings.gateway, s"http://127.0.0.1:$localPort/v1",
			maxParallelRequests = settings.maxParallelRequests)
		
		val startupCommand = {
			if (serviceAutoRuns)
				""
			else {
				// Uses either python or vllm, depending on how vLLM was installed
				val baseCommand = {
					if (shouldInstall)
						s"source ~/miniconda/etc/profile.d/conda.sh && conda activate vllm-env && exec python -m vllm.entrypoints.openai.api_server${
							modelToServe.mapIfNotEmpty { model => s" --model '$model'" } }"
					// TODO: This CUDA_VISIBLE_DEVICES is experimental. Maybe remove
					else
						s"CUDA_VISIBLE_DEVICES=0 exec vllm serve $modelToServe"
				}
				// TODO: Add --tensor-parallel-size N
				s"$baseCommand --max-model-len ${
					_maxContextSize.value } --host 127.0.0.1 --port $remotePort --gpu-memory-utilization ${
					settings.maxGpuUtil }${ settings.extraStartupArgs.prependIfNotEmpty(" ") }"
			}
		}
		
		internalClient -> startupCommand
	}
	
	override protected def attemptServiceConnection(service: LlmServiceClient,
	                                                deprecationView: View[Boolean]): Future[Either[Boolean, OpenAiModelInfo]] =
		service.push(ListOpenAiModels.withDeprecationView(deprecationView)).map { result =>
			result.success.flatMap { _.headOption } match {
				// Case: Models are available => Finishes successfully
				case Some(model) => Right(model)
					
				// Case: No models are available yet => Signals to continue attempts
				case None =>
					if (result.isSuccess)
						debugLog("No models are available yet")
					else
						result.failure.foreach { log(_, "GET /models failed") }
						
					Left(true)
			}
		}
	
	override protected def initializeClient(service: LlmServiceClient, setupResult: OpenAiModelInfo,
	                                        stopFlag: Flag): (LockingRequestQueue, OpenAiModelInfo, TokenCount) =
		(LockingRequestQueue.wrap(service, stopFlag), setupResult, _maxContextSize)
	
	// TODO: Refactor to use scheduler
	override protected def monitorRequestTimeouts(client: (LockingRequestQueue, OpenAiModelInfo,
		TokenCount), service: LlmServiceClient, timedOutFlag: Settable, continueView: View[Boolean]): Unit =
	{
		if (settings.noResponseTimeout.isFinite) {
			val queue = client._1
			// Compares timeout against the earliest request queue time, or the earliest recorded request start time
			// This is in order to avoid timeouts for requests that have been queued (but not running) for a long time
			debugLog(s"${ instanceId.mkString }: Starts monitoring request timeouts")
			val lastRecordedStartTimeP = Volatile(Now.toInstant)
			val process = LoopingProcess.started.after(settings.noResponseTimeout) { _ =>
				queue.pendingRequests.notEmpty match {
					// Case: One or more requests are being executed => Checks if any of them are too old
					case Some(requests) =>
						val earliestRequestTime = requests.iterator.map { _.queueTime }.min max
							lastRecordedStartTimeP.value
						// Case: At least one request has timed out
						//       => Remembers the instance state & requests the API to stop
						if (earliestRequestTime <= Now - settings.noResponseTimeout) {
							debugLog(s"${ instanceId.mkString }: Requests started timing out (${
								(Now - earliestRequestTime).description })")
							timedOutFlag.set()
							None
						}
						// Case: No request has timed out => Updates the start time
						else {
							debugLog(s"${ instanceId.mkString }: No request timed out")
							requests.findMap { request => Some(request.result.startFuture).filterNot { _.isCompleted } }
								.foreach { _.onComplete { _ => lastRecordedStartTimeP.value = Now } }
							
							// Schedules the next check
							Some(earliestRequestTime + settings.noResponseTimeout)
						}
						// Case: No pending requests => No timeout is possible
					case None =>
						debugLog(s"${ instanceId.mkString }: No requests are pending")
						Some(settings.noResponseTimeout)
				}
			}
			// Once this process is requested to stop, timeouts are not needed anymore
			hurryFlag.onceSet { process.stop() }
		}
	}
	
	override protected def stopService(service: LlmServiceClient,
	                                   client: Option[(LockingRequestQueue, OpenAiModelInfo, TokenCount)]): (Future[Any], Changing[Int]) =
		client match {
			// Case: Client initialized => Assumes that it has already been requested to stop
			case Some((client, _, _)) => client.stopFuture -> client.pendingRequestCountPointer
			// Case: No client initialized => No stop is needed
			case None => Future.unit -> Fixed(0)
		}
}
