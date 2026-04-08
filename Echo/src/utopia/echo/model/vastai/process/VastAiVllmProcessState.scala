package utopia.echo.model.vastai.process

import utopia.annex.controller.LockingRequestQueue
import utopia.echo.model.response.openai.OpenAiModelInfo
import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.vastai.instance.InstanceState.{Active, Loading}
import utopia.echo.model.vastai.instance.{InstanceState, VastAiInstance}
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.process.ApiHostingResult.Disconnected
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.{ApiHosting, ApiSetup, InstanceAcquisition, Stopping}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.util.StringExtensions._

import scala.util.Failure

/**
 * An enumeration for different states of an LLM-hosting Vast AI process
 * @author Mikko Hilpinen
 * @since 27.02.2026, v1.5
 */
sealed trait VastAiVllmProcessState
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The process phase, to which this state belongs
	 */
	def phase: VastAiVllmProcessPhase
	
	/**
	 * @return Whether the API is usable in this state
	 */
	def isUsable: Boolean
	
	/**
	 * @return The latest instance state, if an instance is available
	 */
	def availableInstance: Option[VastAiInstance]
	
	/**
	 * @param instance Latest instance state
	 * @return A copy of this state matching that instance state
	 */
	def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether the API is NOT usable in this state
	 */
	def isUnusable = !isUsable
	
	/**
	 * @return Whether a Vast AI instance is available in this process state
	 */
	def isInstanceAvailable = availableInstance.isDefined
}

object VastAiVllmProcessState
{
	// NESTED   --------------------------
	
	sealed trait VastAiVllmProcessPhase extends SelfComparable[VastAiVllmProcessPhase]
	{
		// ABSTRACT ----------------------
		
		/**
		 * @return An ascending index that indicates the overall phase progress
		 */
		def index: Int
		
		/**
		 * @return A short name of this phase in human-readable form
		 */
		def name: String
		
		/**
		 * @return Expected state of the utilized Vast AI instance.
		 *         None if no instance is expected to be present.
		 */
		def expectedInstanceState: Option[InstanceState]
		
		
		// IMPLEMENTED  ------------------
		
		override def self = this
		override def toString = name
		
		override def compareTo(o: VastAiVllmProcessPhase) = index - o.index
	}
	
	object VastAiVllmProcessPhase
	{
		// VALUES   ----------------------
		
		/**
		 * State before the process is started / run is called
		 */
		case object NotStarted extends VastAiVllmProcessPhase with VastAiVllmProcessState
		{
			override val name: String = "not started"
			override val index: Int = 0
			override val isUsable: Boolean = false
			override val availableInstance: Option[VastAiInstance] = None
			override val expectedInstanceState: Option[InstanceState] = None
			
			override def phase: VastAiVllmProcessPhase = this
			
			override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = this
		}
		/**
		 * Phase where the Vast AI instance is being acquired and loaded
		 */
		case object InstanceAcquisition extends VastAiVllmProcessPhase
		{
			override val name: String = "acquiring instance"
			override val index: Int = 1
			override val expectedInstanceState: Option[InstanceState] = Some(Loading)
		}
		/**
		 * Phase where the instance is ready, but the vLLM API is being set up
		 */
		case object ApiSetup extends VastAiVllmProcessPhase
		{
			override val name: String = "setting up API"
			override val index: Int = 2
			override val expectedInstanceState: Option[InstanceState] = Some(Active)
		}
		/**
		 * Phase where the vLLM API has become usable
		 */
		case object ApiHosting extends VastAiVllmProcessPhase
		{
			override val name: String = "hosting API"
			override val index: Int = 3
			override val expectedInstanceState: Option[InstanceState] = Some(Active)
		}
		/**
		 * Phase where the API and the instance are being torn down
		 */
		case object Stopping extends VastAiVllmProcessPhase
		{
			override val name: String = "stopping API"
			override val index: Int = 4
			override val expectedInstanceState: Option[InstanceState] = Some(Active)
		}
		/**
		 * Phase after the process has completed
		 */
		case object Stopped extends VastAiVllmProcessPhase
		{
			override val name: String = "stopped"
			override val index: Int = 5
			override val expectedInstanceState: Option[InstanceState] = None
		}
	}
	
	
	// VALUES   --------------------------
	
	/**
	 * State during which the process is querying and selecting instance offers
	 */
	case object SelectingOffer extends VastAiVllmProcessState
	{
		override val phase: VastAiVllmProcessPhase = InstanceAcquisition
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = None
		
		override def toString = "selecting offer"
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = this
	}
	/**
	 * State at which an offer has been selected, and it's being converted to an instance.
	 * This may include extensive loading, as the instance is being set up.
	 * @param offer Selected offer
	 */
	case class AcquiringInstance(offer: Offer) extends VastAiVllmProcessState
	{
		override val phase: VastAiVllmProcessPhase = InstanceAcquisition
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = None
		
		override def toString = "acquiring instance"
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = InstanceLoading(instance)
	}
	/**
	 * State at which an instance has been created, but is still loading
	 * @param instance The latest state of the acquired instance
	 */
	case class InstanceLoading(instance: VastAiInstance) extends VastAiVllmProcessState
	{
		override val phase: VastAiVllmProcessPhase = InstanceAcquisition
		override val isUsable: Boolean = false
		
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"loading: ${ instance.status }"
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = InstanceLoading(instance)
	}
	/**
	 * State at which the wrapped instance has loaded, but SSH and vLLM may still need to be set up.
	 * @param instance The latest state of the acquired instance
	 */
	case class SettingUpApi(instance: VastAiInstance) extends VastAiVllmProcessState
	{
		// ATTRIBUTES   ---------------------
		
		override val phase: VastAiVllmProcessPhase = ApiSetup
		override val isUsable: Boolean = false
		
		
		// IMPLEMENTED  ----------------------
		
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"setting up API: ${ instance.status }"
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = SettingUpApi(instance)
	}
	/**
	 * State at which the vLLM is being started, or is loading model data, and is not yet responsive.
	 * @param instance The latest state of the acquired instance
	 */
	case class StartingApi(instance: VastAiInstance) extends VastAiVllmProcessState
	{
		override val phase: VastAiVllmProcessPhase = ApiSetup
		override val isUsable: Boolean = false
		
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"starting API: ${ instance.status }"
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = StartingApi(instance)
	}
	/**
	 * State at which the vLLM API is fully functional and usable
	 * @param instance The latest state of the utilized instance
	 * @param apiClient The exposed API client
	 * @param model The usable LLM
	 * @param maxContextSize Maximum context size allowed in this client
	 */
	case class HostingApi(instance: VastAiInstance, apiClient: LockingRequestQueue, model: OpenAiModelInfo,
	                      maxContextSize: TokenCount)
		extends VastAiVllmProcessState
	{
		override val phase: VastAiVllmProcessPhase = ApiHosting
		
		override def isUsable: Boolean = instance.status.instanceIsUsable
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"hosting API${
			Some(instance.status).filterNot { _.instanceIsUsable }.mkString.prependIfNotEmpty(": ") }"
		
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
		
		override val phase: VastAiVllmProcessPhase = Stopping
		override val isUsable: Boolean = false
		
		
		// IMPLEMENTED  ----------------------
		
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"stopping: ${ instance.status }, $requestsPending pending requests"
		
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
		override val phase: VastAiVllmProcessPhase = Stopping
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = None
		
		override def toString = s"destroying: $apiStatus"
		
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
		override val phase: VastAiVllmProcessPhase = VastAiVllmProcessPhase.Stopped
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = None
		
		override def toString = s"stopped: $apiStatus => $finalInstanceProcessState"
		
		override def atInstanceState(instance: VastAiInstance): VastAiVllmProcessState = this
	}
}