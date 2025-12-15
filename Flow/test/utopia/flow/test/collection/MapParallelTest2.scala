package utopia.flow.test.collection

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.{AccessQueue, ActionQueue}
import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.builder.BuildNothing
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.TryCatch
import utopia.flow.view.mutable.eventful.SettableFlag

import scala.util.Random

/**
 * Tests parallel collection-mapping
 * @author Mikko Hilpinen
 * @since 09.12.2025, v2.8
 */
object MapParallelTest2 extends App
{
	val completionFlag = SettableFlag()
	
	private val q = ActionQueue(5)
	
	(1 to 30)
		.parallel.map { i =>
			Wait(Random.nextDouble().seconds)
			println(s"$i")
		}
		.to(BuildNothing.empty).using(new AccessQueue[ActionQueue](q), 10)
		.forResult { result: TryCatch[_] =>
			result match {
				case TryCatch.Success(_, failures) =>
					println(s"Successful completion with ${ failures.size } failures")
					failures.headOption.foreach { _.printStackTrace() }
				case TryCatch.Failure(error) =>
					println("Failure")
					error.printStackTrace()
			}
			println("Completing")
			completionFlag.set()
		}
	
	// q.notPendingFlag.addListener { e => println(s"Queue not pending: $e") }
	// q.queueSizePointer.addListener { e => println(s"Queue size: $e") }
	
	/*
	private val loop = Loop.regularly(3.seconds, waitFirst = true) {
		println(s"Status:\n\t- Queue pending: ${ q.containsPendingActions }\n\t- Pending flag: ${
			q.pendingFlag.value }\n\t- Pending: ${ q.queueSize }\n\t- Empty: ${ q.isEmpty }")
	}*/
	
	// q.pendingFlag.addListener { e => println(s"Pending: $e") }
	
	completionFlag.future.waitFor()
	// loop.stop()
	println("Done!")
}
