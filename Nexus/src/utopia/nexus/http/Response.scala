package utopia.nexus.http

import utopia.flow.generic.casting.ValueConversions._
import utopia.access.model.enumeration.Status._
import utopia.access.model.enumeration.ContentCategory._

import java.io.OutputStream
import java.nio.file
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset
import utopia.access.model.{ContentType, Cookie, Headers}
import utopia.access.model.enumeration.Status
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.util.Mutate
import utopia.nexus.controller.write.WriteResponseBody
import utopia.nexus.controller.write.WriteResponseBody.NoBody
import utopia.nexus.http.Response.CompatibilityWriter
import utopia.nexus.model

import scala.concurrent.Future
import scala.util.Try

@deprecated("Replaced with a new implementation in the model.response package", "v2.0")
object Response
{
    // OTHER METHODS    ----------------
    
    /**
     * Wraps a model body into an UTF-8 encoded JSON response
     * @param body the model that forms the body of the response
     * @param status the status of the response
     */
    def fromModel(body: Model, status: Status = OK, setCookies: Seq[Cookie] = Empty) =
        fromValue(body, status, setCookies)
    /**
     * Wraps a value vector body into an UTF-8 encoded JSON response
     * @param body the vector that forms the body of the response
     * @param status the status of the response
     */
    def fromVector(body: Seq[Value], status: Status = OK, setCookies: Seq[Cookie] = Empty) =
        fromValue(body, status, setCookies)
    /**
     * Wraps a body into an UTF-8 encoded JSON response
     * @param body the value that forms the body of the response
     * @param status the status of the response
     */
    def fromValue(body: Value, status: Status = OK, setCookies: Seq[Cookie] = Empty) =
        new Response(status, Headers.withCurrentDate.withContentType(Application/"json"), setCookies,
            Some(_.write(body.toJson.getBytes(StandardCharsets.UTF_8))))
    
    /**
     * Wraps a file into a response
     * @param filePath A path leading to the target file
     * @param contentType The content type associated with the file. If None (default), the program will 
     * attempt to guess the content type based on the file name.
     * @param status The status for the response. Default OK (200)
     */
    def fromFile(filePath: file.Path, contentType: Option[ContentType] = None, status: Status = OK, 
            setCookies: Seq[Cookie] = Empty) =
    {
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            val contentType = ContentType.guessFrom(filePath.getFileName.toString)
            val headers = if (contentType.isDefined) Headers.withContentType(contentType.get) else Headers.empty
            new Response(status, headers.withCurrentDate, setCookies, Some(Files.copy(filePath, _)))
        }
        else
            new Response(NotFound)
    }
	
	/**
     * Creates an error response
     * @param status the status for the response
     * @param message the optional message sent along with the response
     * @param charset the character set used for encoding the message content. Default = UTF-8
     */
    def plainText(message: String, status: Status = OK, charset: Charset = StandardCharsets.UTF_8) = {
        val headers = Headers.withContentType((Text/"plain").withCharset(charset)).withCurrentDate
        new Response(status, headers, Empty, Some({ _.write(message.getBytes(charset)) }))
    }
    
    /**
     * Creates an empty response with current date header
     * @param status the status for the response (default = 204 = No Content)
     */
    def empty(status: Status = NoContent) = new Response(status, Headers.withCurrentDate)
    
	
	// NESTED   --------------------
	
	private class CompatibilityWriter(headers: Headers, write: OutputStream => Unit) extends WriteResponseBody
	{
		override def self: WriteResponseBody = this
		
		override def isEmpty: Boolean = contentLength.contains(0)
		
		override def contentType: Option[ContentType] = headers.contentType
		override def contentLength: Option[Long] = headers.contentLength
		
		override def writeTo(stream: OutputStream): Future[Try[Unit]] = Future.successful(Try { write(stream) })
	}
}

/**
 * Responses are used for returning data from server side to client side
 * @author Mikko Hilpinen
 * @since 20.8.2017
 * @param status the html status associated with this response. OK by default.
 * @param headers The headers in this response. Empty headers by default.
  * @param setCookies Cookies to be set for the consequent requests
 * @param writeBody a function that writes the response body into a stream. None by default.
 */
@deprecated("Replaced with a new implementation in the model.response package", "v2.0")
class Response(status: Status = OK, headers: Headers = Headers.empty,
               val setCookies: Seq[Cookie] = Empty, val writeBody: Option[OutputStream => Unit] = None)
	extends model.response.Response(status, headers, setCookies, writeBody match {
		case Some(write) => new CompatibilityWriter(headers, write)
		case None => NoBody
	})
{
    // OPERATORS    --------------------
    
    /**
     * Creates a new response with a cookie added to it
     */
    def +(cookie: Cookie) = new Response(status, headers, setCookies :+ cookie, writeBody)
    
    
    // OTHER METHODS    ----------------
    
    /**
      * @param status New status to assign to this response
      * @return Copy of this response with the specified status
      */
    override def withStatus(status: Status) = new Response(status, headers, setCookies, writeBody)
    /**
      * @param f A mapping function applied to this response's status
      * @return Copy of this response with modified status
      */
    override def mapStatus(f: Mutate[Status]) = withStatus(f(status))
    
    /**
      * @param f A mapping function applied to the headers of this response
      * @return Copy of this response with modified headers
      */
    override def mapHeaders(f: Mutate[Headers]) = new Response(status, f(headers), setCookies, writeBody)
    /**
     * Creates a new response with modified headers. The headers are modified in the provided 
     * function
     */
    @deprecated("Renamed to .mapHeaders(...)", "v1.9.3")
    def withModifiedHeaders(modify: Headers => Headers) = mapHeaders(modify)
}