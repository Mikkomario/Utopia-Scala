package utopia.flow.async.process

/**
  * WaitUtils contains a number of utility tools for waiting on a thread. This utility object handles
  * possible interruptedExceptions as well as synchronization
  * @author Mikko Hilpinen
  * @since 8.2.2018
  */
object WaitUtils
{
	// OTHER    ---------------------------
	
	/**
	  * Notifies the lock, so that threads waiting on it will be released
	  * @see #waitUntilNotified(AnyRef)
	  */
	def notify(lock: AnyRef) = lock.synchronized { lock.notifyAll() }
}
