package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.{AccessQueue, ActionQueue}
import utopia.flow.async.process.Delay
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.test.TestContext._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import scala.util.Random

/**
 * Tests multiple parallel mapping operations using a shared queue
 * @author Mikko Hilpinen
 * @since 13.02.2026, v2.8
 */
object SharedParallelMappingTest extends App
{
	private val q = new AccessQueue(ActionQueue(10))
	private val random = new Random()
	private val started = Now.toInstant
	
	private val futures = (1 to 10).map { i1 =>
		println(s"\nMapping coll #$i1")
		(1 to 100).iterator.parallel.async
			.map { i2 => Delay(random.nextDouble().seconds) { i1 * i2 } }
			.using(q, 10)
	}
	
	println("Mapping 10 collections of 100 items in parallel, using 10 threads; ETA 50 seconds.")
	assert(futures.size == 10)
	futures.iterator.zipWithIndex.foreach { case (future, i) =>
		val i1 = i + 1
		if (future.isCompleted) {
			println(s"Future $i1/10 was already completed")
			verifyColl(i1, future.waitForResult().get)
		}
		else {
			println(s"${ (Now - started).description }: Waiting for future #$i1/10")
			val coll = future.waitForResult().get
			println(s"${ (Now - started).description }: Future $i1 finished")
			verifyColl(i1, coll)
		}
	}
	
	println(s"Whole process completed in ${ (Now - started).description }")
	assert(((Now - started) - 50.seconds).abs < 20.seconds)
	
	private def verifyColl(i1: Int, coll: Iterable[Int]) = {
		val collected = coll.toSet
		val expected = (1 to 100).iterator.map { i2 => i1 * i2 }.toSet
		println(s"Collected ${ collected.size }; Expected ${ expected.size }; Additional: ${
			(collected -- expected).toOptimizedSeq.sorted.mkString(":") }; Missing: ${
			(expected -- collected).toOptimizedSeq.sorted.mkString(":") }")
		assert(collected.size == expected.size, collected.size)
		assert(collected == expected, collected.toVector.sorted.mkString(":"))
	}
}
