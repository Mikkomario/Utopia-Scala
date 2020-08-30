package utopia.disciple.http.response

import java.io.InputStream

import utopia.access.http.{Headers, Status}
import utopia.flow.util.AutoClose._

import scala.util.Try

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
    def consume[A](reader: ResponseParser[A]) =
    {
        // Responses must not be read twice
        if (closed)
            throw new IllegalStateException("Response is already consumed")
        else if (isEmpty)
            reader(headers, status)
        else
        {
            openStream match
            {
                case Some(stream) =>
                    closed = true
                    stream.consume { reader(_, headers, status) }
                case None => reader(headers, status)
            }
        }
    }
    
    /**
     * Consumes this response by reading the response body, but only if the response contains a body
     * @param reader the function that is used for parsing the response body.
      * @return Parsed response data. None if response was empty.
     */
    @deprecated("This method is relatively obsolete since the addition of response parsers. This method will be removed in a future release.", "v1.3")
    def consumeIfDefined[A](reader: ResponseParser[A]) = if (isEmpty) None else Some(consume(reader))
    
    /**
     * Buffers this response into program memory, parsing the response contents as well
     * @param parser Response parser used for handling response contents (or the lack of them)
      * @return A buffered (parsed) version of this response
      * @throws IllegalStateException If this response was already consumed
     */
    def buffered[A](parser: ResponseParser[A]) = new BufferedResponse(consume(parser), status, headers/*, cookies*/)
    
    /**
      * Buffers this response into program memory, parsing the response contents
      * @param empty Content that will be used when response is empty
      * @param parser Response content parser. May fail.
      * @tparam A Type of returned content
      * @return Buffered response
      */
    @deprecated("Please use ResponseParser.defaultOnEmpty or ResponseParser.parseOrDefault instead for less ambiguity",
        "v1.3")
    def bufferedOr[A](empty: => A)(parser: (InputStream, Headers, Status) => Try[A]) =
        buffered(ResponseParser.defaultOnEmpty(empty)(parser))
}