package utopia.flow.test.async

import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.eventful.EventfulPointer

import scala.concurrent.ExecutionContext

/**
 * Tests asynchronous change views (AsyncMirror and DelayedView)
 * @author Mikko Hilpinen
 * @since 23.9.2020, v1.9
 */
object AsyncViewTest extends App
{
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("AsyncViewTest")
	val delay = 0.2.seconds
	val waitLock = new AnyRef
	
	// Controlled pointer
	val original = EventfulPointer[Int](0)
	// Pointer that updates with a delay
	val delayed = original.delayedBy(delay)
	// Pointer that slowly adds 1 to the original pointer value and performs a single calculation at a time
	val mirror = original.mapToFuture(0) { i => Delay(delay) { i + 1 } }
	
	// Initial value tests
	assert(original.value == 0)
	assert(delayed.value == 0)
	assert(mirror.value.current == 0)
	
	Wait(delay * 1.2, waitLock)
	
	// Delayed view should now be initialized
	assert(mirror.value.current == 1)
	
	// Updates controlled value, other pointers should be unaffected
	original.value = 1
	
	assert(delayed.value == 0)
	assert(mirror.value.current == 1)
	
	Wait(delay * 0.5, waitLock)
	
	// Makes sure other pointers haven't reacted even after a short delay
	assert(delayed.value == 0)
	assert(mirror.value.current == 1)
	
	Wait(delay * 0.7, waitLock)
	
	// Makes sure the other pointers have been updated after long enough delay
	assert(delayed.value == 1)
	assert(mirror.value.current == 2)
	
	// Performs multiple consecutive changes to the controlled pointer
	var eventsReceived = 0
	val receiveListener: ChangeListener[Any] = _ => { eventsReceived += 1 }
	
	delayed.addListener(receiveListener)
	original.value = 2
	original.value = 3
	original.value = 4
	
	// Makes sure the other pointers haven't yet reacted to any changes
	assert(delayed.value == 1)
	assert(mirror.value.current == 2)
	assert(eventsReceived == 0)
	
	Wait(delay * 1.2, waitLock)
	delayed.removeListener(receiveListener)
	
	// Makes sure the delayed pointer has reacted only once and that the mirror pointer is yet to fully
	// update its value (first calculation should be completed and second underway)
	assert(delayed.value == 4)
	assert(mirror.value.current == 3)
	assert(eventsReceived == 1)
	
	Wait(delay, waitLock)
	
	// Makes sure the mirror pointer also eventually completes
	assert(delayed.value == 4)
	assert(mirror.value.current == 5)
	
	println("Done!")
}
