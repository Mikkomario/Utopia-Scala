package utopia.flow.test.collection

import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

/**
 * Tests pre-polling iterator
 * @author Mikko Hilpinen
 * @since 18.10.2023, v2.3
 */
object PrePollingIteratorTest extends App
{
	import utopia.flow.test.TestContext._
	
	private var t = Now.toInstant
	private val source = Iterator
		.iterate(0) { i =>
			Wait(0.5.seconds)
			i + 1
		}
		.drop(1)
	private val buffered = source.prePollingAsync(2)
	
	assert(buffered.hasNext)
	assert(buffered.next() == 1)
	assert((Now - t) > 0.1.seconds)
	
	Wait(1.0.seconds)
	
	t = Now
	assert(buffered.hasNext)
	assert(buffered.next() == 2)
	assert(Now - t < 0.1.seconds)
	t = Now
	assert(buffered.hasNext)
	assert(buffered.next() == 3)
	assert(Now - t < 0.1.seconds)
	t = Now
	assert(buffered.hasNext)
	assert(buffered.next() == 4)
	assert(Now - t > 0.1.seconds)
	
	println("Success!")
}
