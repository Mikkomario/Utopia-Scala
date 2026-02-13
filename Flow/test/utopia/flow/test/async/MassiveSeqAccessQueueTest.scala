package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.AccessQueue
import utopia.flow.async.process.Delay
import utopia.flow.test.TestContext._
import utopia.flow.time.Now
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.time.TimeExtensions._

/**
 * Tests AccessQueue with multiple accessors
 * @author Mikko Hilpinen
 * @since 13.02.2026, v2.8
 */
object MassiveSeqAccessQueueTest extends App
{
	private val delay = 0.25.seconds
	
	private val p = Volatile(-1)
	private val q = new AccessQueue(p)
	
	private val start = Now.toInstant
	private val futures = (0 until 100).map { i =>
		q { p =>
			val v = p.value
			assert(v == i - 1, s"Expected ${ i - 1 }, got $v")
			Delay(delay) {
				p.update { v =>
					assert(v == i - 1, s"After delay: Expected ${ i - 1 }, got $v")
					i
				}
				i
			}
		}
	}
	assert(futures.size == 100, futures.size)
	
	futures.iterator.zipWithIndex.foreach { case (future, i) =>
		val d1 = Now - start
		val expectedD1 = delay * i
		println(s"\n${ d1.description }: $i init")
		assert((d1 - expectedD1).abs < 2.seconds,
			s"$i init: Expected ${ expectedD1.description }, got ${ d1.description }")
		val result = future.waitFor().get
		val d2 = Now - start
		val expectedD2 = delay * (i + 1)
		println(s"${ d2.description }: $i completion with $result")
		assert(result == i, s"$i: Expected $i, got $result")
		assert((d2 - expectedD2).abs < 2.seconds, s"$i: Expected ${ expectedD2.description }, got ${ d2.description }")
	}
	
	println("Success!")
}
