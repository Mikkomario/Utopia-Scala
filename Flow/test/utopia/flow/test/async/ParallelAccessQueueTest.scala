package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.AccessQueue
import utopia.flow.async.process.Delay
import utopia.flow.test.TestContext._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.Future

/**
 * Tests AccessQueue with multiple parallel accessors
 * @author Mikko Hilpinen
 * @since 13.02.2026, v2.8
 */
object ParallelAccessQueueTest extends App
{
	private val delay = 0.25.seconds
	
	private val p = Volatile(-100)
	private val q = new AccessQueue(p)
	
	private val start = Now.toInstant
	private val futures = (0 until 100).map { i =>
		Future.delegate {
			q { p =>
				println(s"$i accessed the pointer")
				p.value = -i
				Delay(delay) {
					println(s"\t$i finishes with the pointer")
					p.update { v =>
						assert(v == -i, s"$i after delay: Expected ${ -i }, got $v")
						i
					}
					i
				}
			}
		}
	}
	assert(futures.size == 100, futures.size)
	
	futures.iterator.zipWithIndex.foreach { case (future, i) =>
		val d1 = Now - start
		println(s"${ d1.description }: $i init")
		val result = future.waitFor().get
		val d2 = Now - start
		println(s"${ d2.description }: $i completion with $result")
		assert(result == i, s"$i: Expected $i, got $result")
	}
	
	private val totalDuration = Now - start
	println(s"Completed in ${ totalDuration.description }")
	assert((totalDuration - (delay * 100)).abs < 3.seconds)
	
	println("Success!")
}
