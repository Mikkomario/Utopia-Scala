package utopia.disciple.http.response

import utopia.access.http.{Headers, Status}
import utopia.flow.parse.AutoClose._
import utopia.flow.view.mutable.async.VolatileFlag

import java.io.InputStream
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object StreamedResponse
{
	/**
	  * Creates a new response
	  * @param status Response status
	  * @param headers Response headers
	  * @param openStream A function for opening the response body as a stream.
	  *                   Returns None in case this response is empty (no content).
	  * @param close A function for closing are resources that are kept open for this response.
	  *              Called if this response is closed or once the streamed content has been fully processed.
	  * @return A new response
	  */
	def apply(status: Status, headers: Headers)(openStream: => Option[InputStream])(close: => Unit) =
		new StreamedResponse(status, headers)(openStream)(() => close)
}

/**
  * Streamed Responses are Responses that have a limited lifespan and can be consumed once only
  * @author Mikko Hilpinen
  * @since 30.11.2017
  * @param status Response status
  * @param headers Response headers
  * @param openStream Function for opening response stream.
  *                   Returns None if no stream was available (which is treated as an empty response)
  * @param closeDependencies A function which closes any resources that were used in this response, if applicable
  */
class StreamedResponse(override val status: Status, override val headers: Headers)
                      (openStream: => Option[InputStream])(closeDependencies: () => Unit)
	extends Response with AutoCloseable
{
	// ATTRIBUTES    ------------------------
	
	private val consumedFlag = VolatileFlag()
	
	
	// IMPLEMENTED  -------------------------
	
	override def toString = s"$status: Stream(...). Headers: $headers"
	
	override def close() = {
		// Marks this response as consumed and closes any parent resources
		if (consumedFlag.set())
			closeDependencies()
	}
	
	
	// OTHER    ----------------------
	
	/**
	  * Consumes this response by parsing the response body
	  * @param parser A parser used for processing the response body stream
	  * @param exc Implicit execution context (used for closing the stream if the parsing is completed asynchronously)
	  * @throws IllegalStateException If this response has already been consumed before
	  * @tparam A Type of parse results
	  * @return Parse result, including a future which indicates when the underlying response object may be closed /
	  *         when response-parsing has finished.
	  */
	@throws[IllegalStateException]("If this response has already been consumed before")
	def consume[A](parser: ResponseParser[A])(implicit exc: ExecutionContext) = {
		// Closes this response and makes sure it has not been consumed already
		if (consumedFlag.set()) {
			// Opens the response body stream, if applicable
			val stream = openStream
			// Parses the stream contents using the specified parser.
			// Catches possibly throw errors in order to make sure the stream gets closed.
			Try { parser(status, headers, stream) } match {
				case Success(parseResult) =>
					// Schedules stream closing once the parsing completes
					if (parseResult.isCompleted) {
						stream.foreach { _.closeQuietly() }
						closeDependencies()
					}
					else {
						parseResult.parseCompletion.onComplete { _ =>
							stream.foreach { _.closeQuietly() }
							closeDependencies()
						}
					}
					// Returns the parse result
					parseResult
				
				// Case: The parser threw an exception => Closes the stream and forwards the exception to the caller
				case Failure(exception) =>
					stream.foreach { _.closeQuietly() }
					throw exception
			}
		}
		// Case: Already consumed => Throws
		else
			throw new IllegalStateException("This response has already been consumed")
	}
	
	/**
	  * Buffers this response into program memory, parsing the response body
	  * @param parser Parser used for parsing the response body
	  * @param exc Implicit execution used to close the response body stream after asynchronous parsing has completed,
	  *            if applicable
	  * @throws IllegalStateException If this response has already been consumed before
	  * @tparam A Type of response parse results
	  * @return Buffered copy of this response, containing the parse result in place of a response body
	  */
	@throws[IllegalStateException]("If this response has already been consumed before")
	def buffered[A](parser: ResponseParser[A])(implicit exc: ExecutionContext) =
		new BufferedResponse(consume(parser).wrapped, status, headers)
}