package utopia.disciple.http

import utopia.flow.util.AutoClose._
import utopia.access.http.Status
import utopia.access.http.Headers
import java.io.InputStream

import scala.util.{Failure, Success, Try}

/**
 * Streamed Responses are Responses that have a limited lifespan and can be consumed once only
 * @author Mikko Hilpinen
 * @since 30.11.2017
 */
class StreamedResponse(override val status: Status, override val headers: Headers, 
        /*override val cookies: Set[Cookie], */private val openStream: () => InputStream) extends Response
{
    // ATTRIBUTES    ------------------------
    
    private var closed = false
    
    
    // IMPLEMENTED  -------------------------
    
    override def toString = s"$status: Stream(...). Headers: $headers"
    
    
    // OTHER METHODS    ----------------------
    
    /**
     * Consumes this response by reading the response body
     * @param reader the function that is used for parsing the response body. May Fail.
      * @return Reader result. Failure if reader threw and exception or if this response was already consumed or empty.
     */
    def consume[A](reader: (InputStream, Headers) => Try[A]) =
    {
        if (closed)
            Failure(new IllegalStateException("Response is already consumed"))
        else if (isEmpty)
            Failure(new NoContentException("Response is empty"))
        else
        {
            closed = true
            openStream().consume { reader(_, headers) }
        }
    }
    
    /**
     * Consumes this response by reading the response body, but only if the response contains a body
     * @param reader the function that is used for parsing the response body. May throw.
      * @return Parsed response data. None if response was empty. Failure if response was already consumed or if parsing
      *         function threw an exception.
     */
    def consumeIfDefined[A](reader: (InputStream, Headers) => Try[A]) = if (isEmpty) None else Some(consume(reader))
    
    /**
     * Buffers this response into program memory, parsing the response contents as well
     * @param parser the function that is used for parsing the response contents
      * @return A buffered (parsed) version of this response. Contains a failure if this response was empty or
      *         already consumed.
     */
    def buffered[A](parser: (InputStream, Headers) => Try[A]) = new BufferedResponse(consume(parser), status, headers/*, cookies*/)
    
    /**
      * Buffers this response into program memory, parsing the response contents
      * @param empty Content that will be used when response is empty
      * @param parser Response content parser. May fail.
      * @tparam A Type of returned content
      * @return Buffered response
      */
    def bufferedOr[A](empty: => A)(parser: (InputStream, Headers) => Try[A]) = new BufferedResponse(
        consumeIfDefined(parser).getOrElse(Success(empty)), status, headers)
}