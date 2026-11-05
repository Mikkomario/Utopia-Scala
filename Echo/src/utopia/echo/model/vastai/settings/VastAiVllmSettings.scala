package utopia.echo.model.vastai.settings

import utopia.disciple.controller.Gateway
import utopia.disciple.model.request.Timeout
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.vastai.settings.VastAiServiceSettings.{VastAiServiceSettingsFactory, VastAiServiceSettingsWrapper}
import utopia.echo.model.vastai.settings.VastAiVllmSettings.{VastAiVllmSettingsFactory, defaultGateway}
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.language.implicitConversions

object VastAiVllmSettings
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * Default settings
	 */
	lazy val default = apply()
	
	
	// COMPUTED -----------------------
	
	private def defaultGateway = Gateway(maxConnectionsPerRoute = 4, maxConnectionsTotal = 4,
		maximumTimeout = Timeout(read = 10.minutes, manager = 15.minutes), connectionTimeout = 60.seconds,
		disableTrustStoreVerification = true)
	
	
	// IMPLICIT -------------------------
	
	// Implicitly accesses VastAIServiceSettings properties
	implicit def asServiceSettings(vllmSettings: VastAiVllmSettings): VastAiServiceSettings =
		vllmSettings.serviceSettings
		
	
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
		def usingLazyGateway(gateway: => Gateway): Repr = usingLazyGateway(Lazy(gateway))
		/**
		 * @param gateway A Gateway instance to use
		 * @return A copy of this instance using the specified Gateway interface
		 */
		def usingGateway(gateway: Gateway): Repr = usingLazyGateway(View.fixed(gateway))
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
}

/**
 * Specifies settings for constructing Vast AI -hosted vLLM APIs
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
 * @author Mikko Hilpinen
 * @since 05.11.2026, v1.6
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
