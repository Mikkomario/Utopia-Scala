package utopia.flow.async.process

import utopia.flow.async.process.ShutdownReaction.{Cancel, DelayShutdown, SkipDelay}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Flag

import scala.concurrent.ExecutionContext

object DelayedProcess
{
	// OTHER    --------------------------
	
	/**
	  * Creates a new delayed process based on a function call
	  * @param waitTarget Scheduled delay
	  * @param waitLock Wait lock to use (optional)
	  * @param shutdownReaction How this process should react if the JVM is scheduled to shut down during the delay
	  *                         or the processing. (optional)
	  * @param isRestartable Whether this process should be runnable multiple times (default = true)
	  * @param f A function to call after the delay. Accepts a pointer which contains true when the process should be
	  *          hurried to its completion
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A new delayed process instance
	  */
	def apply[U](waitTarget: WaitTarget, waitLock: AnyRef = new AnyRef,
	             shutdownReaction: Option[ShutdownReaction] = None, isRestartable: Boolean = true)
	            (f: => Flag => U)
	            (implicit exc: ExecutionContext, logger: Logger): DelayedProcess =
		new DelayedFunction(waitTarget, waitLock, shutdownReaction, isRestartable)(f)
	
	/**
	  * Creates a new delayed process based on a function call. The process is hurried but not skipped if the JVM
	  * is scheduled to shutdown during this process.
	  * @param waitTarget Scheduled delay
	  * @param waitLock Wait lock to use (optional)
	  * @param isRestartable Whether this process should be runnable multiple times (default = true)
	  * @param f A function to call after the delay. Accepts a pointer which contains true when the process should be
	  *          hurried to its completion
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A new delayed process instance
	  */
	def hurriable[U](waitTarget: WaitTarget, waitLock: AnyRef = new AnyRef, isRestartable: Boolean = true)
	                (f: => Flag => U)
	                (implicit exc: ExecutionContext, logger: Logger) =
		apply(waitTarget, waitLock, Some(SkipDelay), isRestartable = isRestartable)(f)
	
	/**
	  * Creates a new delayed process based on a function call. This function call, including the delay, will be
	  * finished normally even when the JVM is scheduled to shut down
	  * @param waitTarget Scheduled delay
	  * @param waitLock Wait lock to use (optional)
	  * @param isRestartable Whether this process should be runnable multiple times (default = true)
	  * @param f A function to call after the delay
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A new delayed process instance
	  */
	def rigid[U](waitTarget: WaitTarget, waitLock: AnyRef = new AnyRef, isRestartable: Boolean = true)
	            (f: => U)(implicit exc: ExecutionContext, logger: Logger) =
		apply(waitTarget, waitLock, Some(DelayShutdown), isRestartable = isRestartable) { _ => f }
	
	/**
	  * Creates a new delayed process based on a function call. This function call will be skipped in case of a JVM
	  * shutdown, unless the call has started already.
	  * @param waitTarget Scheduled delay
	  * @param waitLock Wait lock to use (optional)
	  * @param isRestartable Whether this process should be runnable multiple times (default = true)
	  * @param f A function to call after the delay. Accepts a pointer which contains true when the process should be
	  *          hurried to its completion
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A new delayed process instance
	  */
	def skippable[U](waitTarget: WaitTarget, waitLock: AnyRef = new AnyRef, isRestartable: Boolean = true)
	                (f: => Flag => U)
	                (implicit exc: ExecutionContext, logger: Logger) =
		apply(waitTarget, waitLock, Some(Cancel), isRestartable = isRestartable)(f)
	
	
	// NESTED   --------------------------
	
	private class DelayedFunction[U](waitTarget: WaitTarget, waitLock: AnyRef,
	                                 shutdownReaction: Option[ShutdownReaction] = None,
	                                 override val isRestartable: Boolean)
	                                (f: => Flag => U)
	                                (implicit exc: ExecutionContext, logger: Logger)
		extends DelayedProcess(waitLock, shutdownReaction)
	{
		override protected def nextDelayTarget = waitTarget
		
		override protected def afterDelay() = f(hurryFlag)
	}
}

/**
  * An abstract class for processes that are delayed before execution
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
abstract class DelayedProcess(waitLock: AnyRef = new AnyRef, shutdownReaction: Option[ShutdownReaction] = None)
                             (implicit exc: ExecutionContext, logger: Logger)
	extends Process(waitLock, shutdownReaction)
{
	// ATTRIBUTES   ----------------------
	
	private val currentWait = ResettableLazy {
		new Wait(nextDelayTarget, waitLock, shutdownReaction, isRestartable = false)
	}
	
	
	// ABSTRACT --------------------------
	
	/**
	  * @return Target for the (next) delay period
	  */
	protected def nextDelayTarget: WaitTarget
	
	/**
	  * This method is called when this process has performed the delay
	  */
	protected def afterDelay(): Unit
	
	
	// IMPLEMENTED  ----------------------
	
	override def stop() = {
		val future = super.stop()
		// Also stops the current waiting process, if necessary
		currentWait.current.foreach { _.stopIfRunning() }
		future
	}
	
	override protected def runOnce() = {
		// Starts with the delay first (which may be interrupted)
		val wait = currentWait.value
		wait.run()
		// If the wait was forcibly interrupted, considers this whole process as broken (InterruptedException case)
		if (wait.state.isBroken && state.isNotBroken)
			markAsInterrupted()
		// Runs the process, unless otherwise determined by state & shutdown rules
		if (state.isNotBroken && (!isShutDown || shutdownReaction.forall { _.finishBeforeShutdown }))
			afterDelay()
		// Prepares another wait instance afterwards
		currentWait.reset()
	}
}
