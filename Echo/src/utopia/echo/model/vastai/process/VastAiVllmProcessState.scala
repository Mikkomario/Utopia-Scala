package utopia.echo.model.vastai.process

import utopia.annex.controller.LockingRequestQueue
import utopia.echo.model.response.openai.OpenAiModelInfo
import utopia.echo.model.vastai.instance.VastAiInstance
import utopia.echo.model.vastai.instance.offer.Offer

/**
 * An enumeration for different states of an LLM-hosting Vast AI process
 * @author Mikko Hilpinen
 * @since 27.02.2026, v1.5
 */
sealed trait VastAiVllmProcessState
{
	// ABSTRACT --------------------------
	
	/**
	 * @return An ascending index that indicates the overall phase progress
	 */
	def phaseIndex: Int
	
	/**
	 * @return Whether the API is usable in this state
	 */
	def isUsable: Boolean
	
	/**
	 * @param instance Latest instance state
	 * @return A copy of this state matching that instance state
	 */
	def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState
}

object VastAiVllmProcessState
{
	// VALUES   --------------------------
	
	/**
	 * State before the process is started / run is called
	 */
	case object NotStarted extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 0
		override val isUsable: Boolean = false
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = this
	}
	/**
	 * State during which the process is querying and selecting instance offers
	 */
	case object SelectingOffer extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 1
		override val isUsable: Boolean = false
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = this
	}
	/**
	 * State at which an offer has been selected, and it's being converted to an instance.
	 * This may include extensive loading, as the instance is being set up.
	 * @param offer Selected offer
	 */
	case class AcquiringInstance(offer: Offer) extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 2
		override val isUsable: Boolean = false
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = InstanceLoading(instance)
	}
	/**
	 * State at which an instance has been created, but is still loading
	 * @param instance The latest state of the acquired instance
	 */
	case class InstanceLoading(instance: VastAiInstance) extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 3
		override val isUsable: Boolean = false
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = InstanceLoading(instance)
	}
	/**
	 * State at which the wrapped instance has technically loaded, but the vLLM API is not yet ready.
	 * May include waiting for the model to load.
	 * @param instance The latest state of the acquired instance
	 */
	case class WaitingForApi(instance: VastAiInstance) extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 4
		override val isUsable: Boolean = false
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = WaitingForApi(instance)
	}
	/**
	 * State at which the vLLM API is fully functional and usable
	 * @param instance The latest state of the utilized instance
	 * @param apiClient The exposed API client
	 * @param model The usable LLM
	 */
	case class HostingApi(instance: VastAiInstance, apiClient: LockingRequestQueue, model: OpenAiModelInfo)
		extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 5
		override def isUsable: Boolean = instance.status.instanceIsUsable
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = copy(instance = instance)
	}
	/**
	 * State at which the vLLM API is being cleared, waiting for pending requests to either succeed or fail.
	 * No further requests are accepted at this point.
	 * @param instance The latest state of the utilized instance
	 * @param requestsPending Number of requests still being processed
	 * @param timedOut Whether the stop was called because requests started to time out (default = false)
	 */
	case class StoppingApi(instance: VastAiInstance, requestsPending: Int, timedOut: Boolean = false)
		extends VastAiVllmProcessState
	{
		// ATTRIBUTES   ----------------------
		
		override val phaseIndex: Int = 6
		override val isUsable: Boolean = false
		
		
		// IMPLEMENTED  ----------------------
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = copy(instance = instance)
		
		
		// OTHER    --------------------------
		
		/**
		 * @param pending Number of requests currently pending / incomplete
		 * @return Copy of this state with the specified request count
		 */
		def withRequestsPending(pending: Int) = copy(requestsPending = pending)
	}
	/**
	 * State at which the Vast AI instance (if one was acquired) is being destroyed.
	 * @param apiStatus Result of the API-hosting attempts
	 */
	case class DestroyingInstance(apiStatus: ApiHostingResult) extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 7
		override val isUsable: Boolean = false
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = this
	}
	/**
	 * State at which the
	 * @param apiStatus Result of the API-hosting attempts
	 * @param finalInstanceProcessState Final state of the utilized VastAiProcess (normally either Terminated or Failed)
	 */
	case class Stopped(apiStatus: ApiHostingResult, finalInstanceProcessState: VastAiProcessState)
		extends VastAiVllmProcessState
	{
		override val phaseIndex: Int = 8
		override val isUsable: Boolean = false
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = this
	}
}