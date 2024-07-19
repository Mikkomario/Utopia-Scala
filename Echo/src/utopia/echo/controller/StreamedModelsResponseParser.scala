package utopia.echo.controller

import utopia.access.http.{Headers, Status}
import utopia.bunnymunch.jawn.AsyncJsonBunny
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.mutable.eventful.LockablePointer
import utopia.flow.view.template.eventful.Changing

import java.io.InputStream
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Try}

/**
  * A response parser which parses items from a streamed json response.
  * Expects the stream to contain a sequence of models separated from each other by whitespace.
  * Only stores the latest received/parsed item at any time.
  * @param parser Parser used for processing the read json objects
  * @param emptyResult Result to return for empty responses
  * @param exc Implicit execution context
  * @tparam A Type of parsed items
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
class StreamedModelsResponseParser[A](parser: FromModelFactory[A], emptyResult: Try[A])(implicit exc: ExecutionContext)
	extends ResponseParser[Changing[Try[A]]]
{
	override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
		stream match {
			case Some(stream) =>
				// Prepares a pointer to store the read values
				val pointer = new LockablePointer(emptyResult)
				
				// Starts reading json data from the stream
				// A flag that is set to true once at least one value has been read,
				// or if reading completes or fails
				val mayProceedFlag = VolatileFlag()
				val resultFuture = AsyncJsonBunny.processStreamedValues(stream) { values =>
					values.lastOption.foreach { value =>
						pointer.value = value.tryModel.flatMap(parser.apply)
						mayProceedFlag.set()
					}
				}
				
				// Processes the final read result once reading completes
				resultFuture.foreachResult { result =>
					result.failure.foreach { error => pointer.value = Failure(error) }
					pointer.lock()
					mayProceedFlag.set()
				}
				
				// Waits until at least one value has been read or process failed, whichever comes first
				mayProceedFlag.future.waitFor()
				
				// Returns the pointer, plus future of stream parsing completion
				ResponseParseResult(pointer.readOnly, resultFuture)
				
			case None => ResponseParseResult.buffered(Fixed(emptyResult))
		}
}
