package utopia.disciple.http.response

import utopia.access.http.{Headers, Status}
import utopia.flow.parse.AutoClose._

import java.io.InputStream

/**
 * Streamed Responses are Responses that have a limited lifespan and can be consumed once only
 * @author Mikko Hilpinen
 * @since 30.11.2017
  * @param status Response status
  * @param headers Response headers
  * @param openStream Function for opening response stream. Returns None if no stream was available
  *                   (which is treated as an empty response)
 */
class StreamedResponse(override val status: Status, override val headers: Headers, 
        /*override val cookies: Set[Cookie], */)(openStream: => Option[InputStream]) extends Response
{
    // ATTRIBUTES    ------------------------
    
    private var closed = false
    
    
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
        if (closed)
            throw new IllegalStateException("Response is already consumed")
        else if (isEmpty)
            reader(headers, status)
        else {
            openStream match {
                case Some(stream) =>
                    closed = true
                    stream.consume { reader(_, headers, status) }
                case None => reader(headers, status)
            }
        }
    }
    
    /**
     * Buffers this response into program memory, parsing the response contents as well
     * @param parser Response parser used for handling response contents (or the lack of them)
      * @return A buffered (parsed) version of this response
      * @throws IllegalStateException If this response was already consumed
     */
    def buffered[A](parser: ResponseParser[A]) = new BufferedResponse(consume(parser), status, headers/*, cookies*/)
}