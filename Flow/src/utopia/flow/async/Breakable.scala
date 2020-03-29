package utopia.flow.async

import scala.concurrent.Future

/**
* Breakable items (operations) can be stopped in the middle of their execution
* @author Mikko Hilpinen
* @since 31.3.2019
**/
trait Breakable
{
    /**
     * Stops this breakable operation
     * @return a furure of the completion of this operation
     */
	def stop(): Future[Unit]
	
	/**
	  * Registers this breakable item to stop once / before the JVM closes
	  */
	def registerToStopOnceJVMCloses() = CloseHook += this
}