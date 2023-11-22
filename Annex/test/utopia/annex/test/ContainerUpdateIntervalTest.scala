package utopia.annex.test

import utopia.access.http.{Headers, Status}
import utopia.access.http.Status.OK
import utopia.annex.controller.ContainerUpdateLoop
import utopia.annex.model.response.{RequestResult, Response, ResponseBody}
import utopia.flow.async.process.Wait
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.parse.file.container.{FileContainer, SaveTiming, ValueConvertibleOptionFileContainer, ValueFileContainer}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.time.TimeExtensions._

import java.nio.file.Path
import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Tests last update time logic in ContainerUpdateLoop
  * @author Mikko Hilpinen
  * @since 21.11.2023, v1.6.1
  */
object ContainerUpdateIntervalTest extends App
{
	import utopia.flow.test.TestContext._
	Status.setup()
	
	
	// ATTRIBUTES   ----------------
	
	implicit val jsonParser: JsonParser = JsonReader
	
	private val dataDir: Path = "Annex/data/test-data"
	
	private val timeIterator = Iterator.iterate(Instant.EPOCH) { _ + 10.seconds }
	
	private var lastTime = timeIterator.next()
	
	
	// TESTS    --------------------
	
	TestLoop.runAsync()
	Wait(12.seconds)
	TestLoop.stop()
	println("Success!")
	
	
	// NESTED   --------------------
	
	private object TestLoop extends ContainerUpdateLoop(
		new ValueFileContainer(dataDir/"test-container.json", SaveTiming.OnlyOnTrigger))
	{
		// ATTRIBUTES   ------------
		
		override val standardUpdateInterval: FiniteDuration = 1.seconds
		
		override protected lazy val requestTimeContainer: FileContainer[Option[Instant]] =
			new ValueConvertibleOptionFileContainer[Instant](dataDir/"test-container-update-time.json")
			
		
		// INITIAL CODE -----------
		
		requestTimeContainer.current = Some(lastTime)
		
		
		// IMPLEMENTED  -----------
		
		override protected def makeRequest(timeThreshold: Option[Instant]): Future[RequestResult] = {
			println(s"since = $timeThreshold, expecting $lastTime")
			assert(timeThreshold.contains(lastTime))
			Future {
				Wait(2.seconds)
				val time = timeIterator.next()
				lastTime = time
				Response.Success(OK, ResponseBody(time), Headers.withDate(time))
			}
		}
		
		override protected def handleFailureResponse(status: Status, message: Option[String]): Unit =
			throw new IllegalStateException("Failures not allowed")
		
		override protected def merge(oldData: Value, readData: ResponseBody): (Value, FiniteDuration) =
			readData.value -> standardUpdateInterval
	}
}
