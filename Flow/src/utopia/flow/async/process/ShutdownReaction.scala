package utopia.flow.async.process

/**
  * An enumeration for different approaches which may be taken when an operation is yet to be performed on system
  * shutdown.
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
sealed trait ShutdownReaction
{
	/**
	  * @return Whether the associated action should be completed before jvm shuts down, delaying the shutdown process
	  */
	def finishBeforeShutdown: Boolean
	/**
	  * @return Whether a possible waiting process should be terminated upon jvm shutdown
	  */
	def skipWait: Boolean
}

object ShutdownReaction
{
	/**
	  * A reaction where the shutdown is delayed until the associated process has completed.
	  * No waiting will be skipped in order to speed up the process.
	  */
	case object DelayShutdown extends ShutdownReaction
	{
		override def finishBeforeShutdown = true
		override def skipWait = false
	}
	/**
	  * A reaction where the shutdown is delayed until the associated process has completed.
	  * The process will be started immediately when the shutdown initiates, if not started already.
	  */
	case object SkipDelay extends ShutdownReaction
	{
		override def finishBeforeShutdown = true
		override def skipWait = true
	}
	/**
	  * A reaction that cancels the pending process when the shutdown process initiates, provided that the
	  * process hasn't started already.
	  */
	case object Cancel extends ShutdownReaction
	{
		override def finishBeforeShutdown = false
		override def skipWait = true
	}
}