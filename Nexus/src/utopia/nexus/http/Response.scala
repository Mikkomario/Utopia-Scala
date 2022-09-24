package utopia.nexus.http

import utopia.flow.generic.casting.ValueConversions._
import utopia.access.http.Status._
import utopia.access.http.ContentCategory._

import java.io.OutputStream
import java.nio.file
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset
import utopia.access.http.Status
import utopia.access.http.Cookie
import utopia.access.http.Headers
import utopia.access.http.ContentType
import utopia.flow.collection.value.typeless.Value
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.model.immutable.{Model, Value}

object Response
{
    // OTHER METHODS    ----------------
    
    /**
     * Wraps a model body into an UTF-8 encoded JSON response
     * @param body the model that forms the body of the response
     * @param status the status of the response
     */
    def fromModel(body: Model, status: Status = OK, setCookies: Seq[Cookie] = Vector()) =
        fromValue(body, status, setCookies)
    
    /**
     * Wraps a value vector body into an UTF-8 encoded JSON response
     * @param body the vector that forms the body of the response
     * @param status the status of the response
     */
    def fromVector(body: Vector[Value], status: Status = OK, setCookies: Seq[Cookie] = Vector()) =
        fromValue(body, status, setCookies)
    
    /**
     * Wraps a body into an UTF-8 encoded JSON response
     * @param body the value that forms the body of the response
     * @param status the status of the response
     */
    def fromValue(body: Value, status: Status = OK, setCookies: Seq[Cookie] = Vector()) =
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
            setCookies: Seq[Cookie] = Vector()) = 
    {
        if (Files.exists(filePath) && !Files.isDirectory(filePath))
        {
            val contentType = ContentType.guessFrom(filePath.getFileName.toString)
            val headers = if (contentType.isDefined) Headers.withContentType(contentType.get) else Headers.empty
            new Response(status, headers.withCurrentDate, setCookies, Some(Files.copy(filePath, _)))
        }
        else
        {
            new Response(NotFound)
        }
    }
    
    /**
     * Creates an error response
     * @param status the status for the response
     * @param message the optional message sent along with the response
     * @param charset the character set used for encoding the message content. Default = UTF-8
     */
    def plainText(message: String, status: Status = OK, charset: Charset = StandardCharsets.UTF_8) = 
    {
        val headers = Headers.withContentType(Text/"plain", Some(charset)).withCurrentDate
        new Response(status, headers, Vector(), Some({ _.write(message.getBytes(charset)) }))
    }
    
    /**
     * Creates an empty response with current date header
     * @param status the status for the response
     */
    def empty(status: Status = OK) = new Response(status, Headers.withCurrentDate)
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
class Response(val status: Status = OK, val headers: Headers = Headers.empty,
        val setCookies: Seq[Cookie] = Vector(), val writeBody: Option[OutputStream => Unit] = None)
{
    // OPERATORS    --------------------
    
    /**
     * Creates a new response with a cookie added to it
     */
    def +(cookie: Cookie) = new Response(status, headers, setCookies :+ cookie, writeBody)
    
    
    // OTHER METHODS    ----------------
    
    /**
     * Creates a new response with modified headers. The headers are modified in the provided 
     * function
     */
    def withModifiedHeaders(modify: Headers => Headers) = new Response(status, modify(headers), setCookies, writeBody)
}