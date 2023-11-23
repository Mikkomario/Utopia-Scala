package utopia.annex.test

import utopia.access.http.Status.NoContent
import utopia.access.http.{Headers, Status}
import utopia.annex.controller.ContainerUpdateLoop
import utopia.annex.model.response.ResponseBody.Empty
import utopia.annex.model.response.{RequestResult, Response, ResponseBody}
import utopia.flow.async.process.ProcessState.Stopped
import utopia.flow.async.process.Wait
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.container.SaveTiming.OnlyOnTrigger
import utopia.flow.parse.file.container.{FileContainer, SaveTiming, ValueConvertibleOptionFileContainer, ValueFileContainer}
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.Volatile

import java.nio.file.Path
import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Tests manual container update triggering
  * @author Mikko Hilpinen
  * @since 21.11.2023, v1.6.1
  */
object ContainerUpdateTimeManipulationTest extends App
{
	import utopia.flow.test.TestContext._
	Status.setup()
	
	
	// ATTRIBUTES   ----------------
	
	implicit val jsonParser: JsonParser = JsonReader
	private val dataDir: Path = "Annex/data/test-data"
	
	private val counter = Volatile(0)
	
	
	// TESTS    --------------------
	
	TestLoop.runAsync()
	Wait(1.seconds)
	assert(counter.value == 1)
	TestLoop.skipWait()
	Wait(1.seconds)
	assert(counter.value == 2)
	TestLoop.skipWait()
	Wait(1.seconds)
	assert(counter.value == 3)
	Wait(1.seconds)
	assert(counter.value == 3)
	TestLoop.stop()
	Wait(1.seconds)
	assert(counter.value == 3)
	assert(TestLoop.state == Stopped)
	
	println("Success!")
	
	
	// NESTED   --------------------
	
	private object TestLoop extends ContainerUpdateLoop(
		new ValueFileContainer(dataDir/"test-container.json", SaveTiming.OnlyOnTrigger))
	{
		// ATTRIBUTES   ------------
		
		override val standardUpdateInterval: FiniteDuration = 10.seconds
		
		override protected lazy val requestTimeContainer: FileContainer[Option[Instant]] =
			new ValueConvertibleOptionFileContainer[Instant](dataDir/"test-container-update-time.json",
				OnlyOnTrigger)
		
		
		// IMPLEMENTED  -----------
		
		override protected def makeRequest(timeThreshold: Option[Instant]): Future[RequestResult] = {
			Future {
				counter.update { _ + 1 }
				Response.Success(NoContent, Empty, Headers.empty)
			}
		}
		
		override protected def handleFailureResponse(status: Status, message: Option[String]): Unit =
			throw new IllegalStateException("Failures not allowed")
		
		override protected def merge(oldData: Value, readData: ResponseBody): (Value, FiniteDuration) =
			readData.value -> standardUpdateInterval
	}
}
