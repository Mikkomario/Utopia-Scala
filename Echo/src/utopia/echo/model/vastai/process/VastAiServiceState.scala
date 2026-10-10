package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.InstanceState.{Active, Loading}
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.instance.{InstanceState, VastAiInstance}
import utopia.echo.model.vastai.process.VastAiServiceState.VastAiServicePhase
import utopia.echo.model.vastai.process.VastAiServiceState.VastAiServicePhase.{InstanceAcquisition, Serving, Setup, Stopping}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.util.StringExtensions._

/**
 * An enumeration for different states of a service-hosting Vast AI process
 * @author Mikko Hilpinen
 * @since 27.02.2026, v1.5
 */
sealed trait VastAiServiceState
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The process phase to which this state belongs
	 */
	def phase: VastAiServicePhase
	
	/**
	 * @return Whether the service may be used in this state
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
	def atInstanceState(instance: VastAiInstance): VastAiServiceState
	
	
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

object VastAiServiceState
{
	// NESTED   --------------------------
	
	sealed trait VastAiServicePhase extends SelfComparable[VastAiServicePhase]
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
		
		override def compareTo(o: VastAiServicePhase) = index - o.index
	}
	
	object VastAiServicePhase
	{
		// VALUES   ----------------------
		
		/**
		 * State before the process is started / run is called
		 */
		case object NotStarted extends VastAiServicePhase with VastAiServiceState
		{
			// ATTRIBUTES  ---------------
			
			override val name: String = "not started"
			override val index: Int = 0
			override val isUsable: Boolean = false
			override val availableInstance: Option[VastAiInstance] = None
			override val expectedInstanceState: Option[InstanceState] = None
			
			
			// IMPLEMENTED  ---------------
			
			override def phase: VastAiServicePhase = this
			
			override def atInstanceState(instance: VastAiInstance): VastAiServiceState = this
		}
		/**
		 * Phase where the Vast AI instance is being acquired and loaded
		 */
		case object InstanceAcquisition extends VastAiServicePhase
		{
			override val name: String = "acquiring instance"
			override val index: Int = 1
			override val expectedInstanceState: Option[InstanceState] = Some(Loading)
		}
		/**
		 * Phase where the instance is ready, but the service is being set up
		 */
		case object Setup extends VastAiServicePhase
		{
			override val name: String = "setting up the service"
			override val index: Int = 2
			override val expectedInstanceState: Option[InstanceState] = Some(Active)
		}
		/**
		 * Phase where the service has become usable
		 */
		case object Serving extends VastAiServicePhase
		{
			override val name: String = "hosting"
			override val index: Int = 3
			override val expectedInstanceState: Option[InstanceState] = Some(Active)
		}
		/**
		 * Phase where the service and the instance are being torn down
		 */
		case object Stopping extends VastAiServicePhase
		{
			override val name: String = "stopping the service"
			override val index: Int = 4
			override val expectedInstanceState: Option[InstanceState] = Some(Active)
		}
		/**
		 * Phase after the process has completed
		 */
		case object Stopped extends VastAiServicePhase
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
	case object SelectingOffer extends VastAiServiceState
	{
		override val phase: VastAiServicePhase = InstanceAcquisition
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = None
		
		override def toString = "selecting offer"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = this
	}
	/**
	 * State at which an offer has been selected, and it's being converted to an instance.
	 * This may include extensive loading, as the instance is being set up.
	 * @param offer Selected offer
	 */
	case class AcquiringInstance(offer: Offer) extends VastAiServiceState
	{
		override val phase: VastAiServicePhase = InstanceAcquisition
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = None
		
		override def toString = "acquiring instance"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = InstanceLoading(instance)
	}
	/**
	 * State at which an instance has been created, but is still loading
	 * @param instance The latest state of the acquired instance
	 */
	case class InstanceLoading(instance: VastAiInstance) extends VastAiServiceState
	{
		override val phase: VastAiServicePhase = InstanceAcquisition
		override val isUsable: Boolean = false
		
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"loading: ${ instance.status }"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = InstanceLoading(instance)
	}
	/**
	 * State at which the wrapped instance has loaded, but SSH and the service may still need to be set up.
	 * @param instance The latest state of the acquired instance
	 */
	case class SettingUp(instance: VastAiInstance) extends VastAiServiceState
	{
		// ATTRIBUTES   ---------------------
		
		override val phase: VastAiServicePhase = Setup
		override val isUsable: Boolean = false
		
		
		// IMPLEMENTED  ----------------------
		
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"setting up API: ${ instance.status }"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = SettingUp(instance)
	}
	/**
	 * State at which the service is fully functional and usable
	 * @param instance The latest state of the utilized instance
	 */
	case class Hosting(instance: VastAiInstance) extends VastAiServiceState
	{
		override val phase: VastAiServicePhase = Serving
		
		override def isUsable: Boolean = instance.status.instanceIsUsable
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"hosting ${
			Some(instance.status).filterNot { _.instanceIsUsable }.mkString.prependIfNotEmpty(": ") }"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = copy(instance = instance)
	}
	/**
	 * State at which the service is being cleared / torn down, waiting for pending requests to either succeed or fail.
	 * No further requests are accepted at this point.
	 * @param instance The latest state of the utilized instance
	 * @param requestsPending Number of requests still being processed
	 * @param timedOut Whether the stop was called because requests started to time out (default = false)
	 */
	case class StoppingService(instance: VastAiInstance, requestsPending: Int, timedOut: Boolean = false)
		extends VastAiServiceState
	{
		// ATTRIBUTES   ----------------------
		
		override val phase: VastAiServicePhase = Stopping
		override val isUsable: Boolean = false
		
		
		// IMPLEMENTED  ----------------------
		
		override def availableInstance: Option[VastAiInstance] = Some(instance)
		
		override def toString = s"stopping: ${ instance.status }, $requestsPending pending requests"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = copy(instance = instance)
		
		
		// OTHER    --------------------------
		
		/**
		 * @param pending Number of requests currently pending / incomplete
		 * @return Copy of this state with the specified request count
		 */
		def withRequestsPending(pending: Int) = copy(requestsPending = pending)
	}
	/**
	 * State at which the Vast AI instance (if one was acquired) is being stopped or destroyed.
	 * @param apiStatus Result of the API-hosting attempts
	 * @param instance The instance that's being stopped or destroyed. None if no instance was acquired.
	 * @param destroying Whether the instance is being destroyed. False if it's only being stopped.
	 */
	case class StoppingInstance(apiStatus: HostingResult, instance: Option[VastAiInstance], destroying: Boolean)
		extends VastAiServiceState
	{
		override val phase: VastAiServicePhase = Stopping
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = if (destroying) None else instance
		
		override def toString = s"${ if (destroying) "destroying" else "stopping" }: $apiStatus"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = copy(instance = Some(instance))
	}
	/**
	 * State at which the underlying Vast AI instance has been destroyed, stopped, or failed to be destroyed.
	 * @param apiStatus Result of the API-hosting attempts
	 * @param finalInstanceProcessState Final state of the utilized VastAiProcess (normally either Terminated or Failed)
	 * @param instance The last acquired state of the rented Vast AI instance. None if no instance was acquired.
	 * @param destroyed Whether the instance was properly destroyed.
	 *                  False if only stopped, or if failed to destroy or stop the instance
	 *                  (in which case the instance might still be active).
	 */
	case class Stopped(apiStatus: HostingResult, finalInstanceProcessState: VastAiProcessState,
	                   instance: Option[VastAiInstance], destroyed: Boolean)
		extends VastAiServiceState
	{
		override val phase: VastAiServicePhase = VastAiServicePhase.Stopped
		override val isUsable: Boolean = false
		override val availableInstance: Option[VastAiInstance] = if (destroyed) None else instance
		
		override def toString = s"stopped: $apiStatus => $finalInstanceProcessState"
		
		override def atInstanceState(instance: VastAiInstance): VastAiServiceState = copy(instance = Some(instance))
	}
}