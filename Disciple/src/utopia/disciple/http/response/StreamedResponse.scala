package utopia.disciple.http.response

import utopia.access.http.{Headers, Status}
import utopia.disciple.http.response.parser.ResponseParser2
import utopia.flow.parse.AutoClose._
import utopia.flow.view.mutable.async.VolatileFlag

import java.io.InputStream
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * Streamed Responses are Responses that have a limited lifespan and can be consumed once only
 * @author Mikko Hilpinen
 * @since 30.11.2017
  * @param status Response status
  * @param headers Response headers
  * @param openStream Function for opening response stream.
  *                   Returns None if no stream was available (which is treated as an empty response)
 */
class StreamedResponse(override val status: Status, override val headers: Headers)
                      (openStream: => Option[InputStream])
    extends Response
{
    // ATTRIBUTES    ------------------------
    
    private val consumedFlag = VolatileFlag()
    
    
    // IMPLEMENTED  -------------------------
    
    override def toString = s"$status: Stream(...). Headers: $headers"
    
    
    // OTHER METHODS    ----------------------
    
    /**
     * Consumes this response by reading the response body
     * @param reader Response parser
      * @return Reader result
      * @throws IllegalStateException If this response was already consumed
     */
    def consume[A](reader: ResponseParser[A]) = {
        // Responses must not be read twice
        if (consumedFlag.getAndSet())
            throw new IllegalStateException("Response is already consumed")
        else if (isEmpty)
            reader(headers, status)
        else
            openStream match {
                case Some(stream) => stream.consume { reader(_, headers, status) }
                case None => reader(headers, status)
            }
    }
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
    def consume[A](parser: ResponseParser2[A])(implicit exc: ExecutionContext) = {
        // Closes this response and makes sure it has not been consumed already
        if (consumedFlag.set()) {
            // Opens the response body stream, if applicable
            val stream = openStream
            // Parses the stream contents using the specified parser.
            // Catches possibly throw errors in order to make sure the stream gets closed.
            Try { parser(status, headers, stream) } match {
                case Success(parseResult) =>
                    // Schedules stream closing once the parsing completes
                    stream.foreach(parseResult.closeOnCompletion)
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
     * Buffers this response into program memory, parsing the response contents as well
     * @param parser Response parser used for handling response contents (or the lack of them)
      * @return A buffered (parsed) version of this response
      * @throws IllegalStateException If this response was already consumed
     */
    def buffered[A](parser: ResponseParser[A]) = new BufferedResponse(consume(parser), status, headers)
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
    def buffered[A](parser: ResponseParser2[A])(implicit exc: ExecutionContext) =
        new BufferedResponse(consume(parser), status, headers)
}