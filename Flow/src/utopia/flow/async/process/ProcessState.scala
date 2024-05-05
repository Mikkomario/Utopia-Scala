package utopia.flow.async.process

/**
  * An enumeration for representing different states a breakable + runnable item may have
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
sealed trait ProcessState
{
	// ABSTRACT ---------------------
	
	/**
	  * @return Whether the process has started in the past (isn't necessarily still running, however)
	  */
	def hasStarted: Boolean
	/**
	  * @return Whether the process has been stopped, cancelled or broken at some point
	  */
	def isBroken: Boolean
	/**
	  * @return Whether the process is currently running
	  */
	def isRunning: Boolean
	
	/**
	  * @return The state that follows when this one is broken
	  */
	def broken: ProcessState
	
	
	// COMPUTED --------------------
	
	/**
	  * @return Whether the process hasn't (even) started yet
	  */
	def hasNotStarted = !hasStarted
	/**
	  * @return Whether the process is not currently running
	  */
	def isNotRunning = !isRunning
	/**
	  * @return Whether the process hasn't been broken at this time
	  */
	def isNotBroken = !isBroken
	
	/**
	  * @return Whether this is the final state of the process
	  */
	def isFinal = (hasStarted || isBroken) && !isRunning
	/**
	  * @return Whether this is not the final state of the process
	  */
	def isNotFinal = !isFinal
}

object ProcessState
{
	/**
	  * Common trait for the "basic" process states, which don't include features around breaking
	  * (i.e. forcefully stopping) or looping
	  */
	sealed trait BasicProcessState extends ProcessState
	/**
	  * Common trait for the more advanced process states that rely on the features of breaking and/or looping
	  */
	sealed trait AdvancedProcessState extends ProcessState
	
	/**
	  * State before the process has started (nor stopped / cancelled)
	  */
	case object NotStarted extends BasicProcessState
	{
		override def hasStarted = false
		override def isBroken = false
		override def isRunning = false
		
		override def broken = Cancelled
	}
	/**
	  * State when the process was stopped before it started
	  */
	case object Cancelled extends AdvancedProcessState
	{
		override def hasStarted = false
		override def isBroken = true
		override def isRunning = false
		
		override def broken = this
	}
	/**
	  * State where the process is running normally
	  */
	case object Running extends BasicProcessState
	{
		override def hasStarted = true
		override def isBroken = false
		override def isRunning = true
		
		override def broken = Stopping
	}
	/**
	  * State where the process is running, and is requested to rerun afterwards
	  */
	case object Looping extends AdvancedProcessState
	{
		override def hasStarted = true
		override def isBroken = false
		override def isRunning = true
		
		override def broken = Stopping
	}
	/**
	  * State where the process has been broken and is in the process of terminating
	  */
	case object Stopping extends AdvancedProcessState
	{
		override def hasStarted = true
		override def isBroken = true
		override def isRunning = true
		
		override def broken = this
	}
	/**
	  * State where the process was broken and has been terminated
	  */
	case object Stopped extends AdvancedProcessState
	{
		override def hasStarted = true
		override def isBroken = true
		override def isRunning = false
		
		override def broken = this
	}
	/**
	  * State where the process finished naturally
	  */
	case object Completed extends BasicProcessState
	{
		override def hasStarted = true
		override def isBroken = false
		override def isRunning = false
		
		override def broken = this
	}
}