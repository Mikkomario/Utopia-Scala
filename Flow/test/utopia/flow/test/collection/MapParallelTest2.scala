package utopia.flow.test.collection

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.test.TestContext._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.builder.BuildNothing
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
	
	(1 to 30)
		.mapParallelTo(BuildNothing.empty, 3) { i =>
			Wait(Random.nextDouble().seconds)
			println(s"$i")
		}
		.foreachResult { result: TryCatch[_] =>
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
	
	completionFlag.future.waitFor()
	println("Done!")
}
