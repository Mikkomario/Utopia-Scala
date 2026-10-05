package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.InstanceState.Active
import utopia.echo.model.vastai.instance.InstanceStatus
import utopia.flow.async.process.ProcessState
import utopia.flow.view.template.Extender

/**
 * An enumeration for different states of a Vast AI process
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
sealed trait VastAiProcessState extends Extender[ProcessState]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Whether the instance is usable in this state
	 */
	def isUsable: Boolean
	/**
	 * @return Whether the instance should be continued to be used in this state.
	 *         False if the instance is unusable OR if the instance may become unusable in the near future.
	 */
	def shouldBeUsed: Boolean
	/**
	 * @return Whether the instance may become usable from this state.
	 *         Also returns true if the instance is already (fully) usable.
	 */
	def mayBecomeUsable: Boolean
	
	/**
	 * @return The current instance status, if known
	 */
	def instanceStatus: Option[InstanceStatus]
	
	/**
	 * @param status An instance status
	 * @return Copy of this state with the specified status
	 */
	def withInstanceStatus(status: InstanceStatus): VastAiProcessState
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Whether the instance is not usable in this state
	 */
	def isUnusable: Boolean = !isUsable
	
	/**
	 * @return Whether the instance is not and will not become usable anymore
	 */
	def wontBeUsable = !mayBecomeUsable
}

object VastAiProcessState
{
	// VALUES   -------------------------
	
	/**
	 * State before staring the process. No costs are accumulating at this point.
	 */
	case object NotStarted extends VastAiProcessState
	{
		override val wrapped: ProcessState = ProcessState.NotStarted
		override val instanceStatus: Option[InstanceStatus] = None
		override val isUsable: Boolean = false
		override val shouldBeUsed: Boolean = false
		override val mayBecomeUsable: Boolean = true
		
		override def withInstanceStatus(status: InstanceStatus): VastAiProcessState = {
			if (status.actual.value == Active)
				Running(status)
			else
				this
		}
	}
	
	/**
	 * State while looking for and accepting an offer, and possibly while loading the instance
	 * (depending on Vast AI configuration).
	 */
	case object Starting extends VastAiProcessState
	{
		override val wrapped: ProcessState = ProcessState.Running
		override val instanceStatus: Option[InstanceStatus] = None
		override val isUsable: Boolean = false
		override val shouldBeUsed: Boolean = false
		override val mayBecomeUsable: Boolean = true
		
		override def withInstanceStatus(status: InstanceStatus): VastAiProcessState = {
			if (status.actual.value == Active)
				Running(status)
			else
				this
		}
	}
	/**
	 * State while the instance is active, and termination has not been initiated.
	 * Note: The instance may still be loading at this point.
	 * @param status Current instance status
	 */
	case class Running(status: InstanceStatus) extends VastAiProcessState
	{
		override val wrapped: ProcessState = ProcessState.Running
		override val instanceStatus: Option[InstanceStatus] = Some(status)
		override val isUsable: Boolean = status.instanceIsUsable
		override val shouldBeUsed: Boolean = status.instanceShouldBeUsed
		override val mayBecomeUsable: Boolean = true
		
		override def withInstanceStatus(status: InstanceStatus): VastAiProcessState = Running(status)
	}
	/**
	 * Status while the instance is being stopped or destroyed
	 * @param instanceStatus Current instance status, if available
	 * @param destroying Whether the instance is being destroyed.
	 *                   False if only stopping the instance.
	 */
	case class Stopping(instanceStatus: Option[InstanceStatus], destroying: Boolean) extends VastAiProcessState
	{
		// ATTRIBUTES   ----------------------
		
		override val wrapped: ProcessState = ProcessState.Stopping
		override val shouldBeUsed: Boolean = false
		override val mayBecomeUsable: Boolean = false
		
		
		// IMPLEMENTED  ----------------------
		
		override def isUsable: Boolean = instanceStatus.exists { _.instanceIsUsable }
		
		override def withInstanceStatus(status: InstanceStatus): VastAiProcessState =
			copy(instanceStatus = Some(status))
	}
	/**
	 * Status after the instance has been successfully stopped or destroyed
	 * @param destroyed Whether the instance was destroyed. False if the instance was only stopped.
	 */
	case class Terminated(destroyed: Boolean) extends VastAiProcessState
	{
		override val wrapped: ProcessState = ProcessState.Stopped
		override val instanceStatus: Option[InstanceStatus] = None
		override val isUsable: Boolean = false
		override val shouldBeUsed: Boolean = false
		override val mayBecomeUsable: Boolean = false
		
		override def withInstanceStatus(status: InstanceStatus): VastAiProcessState = this
	}
	
	/**
	 * Status that follows a failure to terminate, or a failure to acquire an instance.
	 * @param cause Cause of this failure
	 * @param previousStatus Status immediately before this failure
	 * @param remainingInstanceId ID of an instance that couldn't be terminated.
	 *                            The presence of this indicates that costs may be incurring in the background.
	 *                            None if no active instance remains.
	 */
	case class Failed(cause: Throwable, previousStatus: VastAiProcessState, remainingInstanceId: Option[Int] = None)
		extends VastAiProcessState
	{
		// ATTRIBUTES   ----------------------
		
		override val wrapped: ProcessState = ProcessState.Completed
		override val instanceStatus: Option[InstanceStatus] = None
		override val isUsable: Boolean = false
		override val shouldBeUsed: Boolean = false
		override val mayBecomeUsable: Boolean = false
		
		
		// IMPLEMENTED  ----------------------
		
		override def withInstanceStatus(status: InstanceStatus): VastAiProcessState = this
	}
}