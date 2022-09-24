package utopia.disciple.http.request

import java.io.{InputStream, OutputStream}
import java.nio.charset.Charset

import utopia.access.http.ContentType
import utopia.flow.parse.AutoClose._

import scala.util.Try

/**
* A body can be placed in an http request
* @author Mikko Hilpinen
* @since 1.5.2018
**/
trait Body
{
    // ABSTRACT    -------------------------
    
    /**
     * Whether this body can repeat it's stream contents multiple times
     */
	def repeatable: Boolean
	/**
	 * Whether this data in this body should be chunked
	 */
	def chunked: Boolean
	
	/**
	 * The length of the body content in bytes, if appliable
	 */
	def contentLength: Option[Long]
	/**
	 * The content type of this body
	 */
	def contentType: ContentType
	/**
	 * The character set used in this body, if applicable
	 */
	def charset: Option[Charset]
	/**
	 * The content encoding used, if applicable
	 */
	def contentEncoding: Option[String]
	
	/**
	 * The content stream. May fail.
	 */
	def stream: Try[InputStream]
	
	
	// OTHER METHODS    ---------------------
	
	/**
	 * Writes the contents of this body's stream into an output stream. Best performance is
	 * achieved if both streams are buffered.
	 */
	def writeTo(output: OutputStream) =
	{
	    // See: https://stackoverflow.com/questions/6927873/
	    // how-can-i-read-a-file-to-an-inputstream-then-write-it-into-an-outputstream-in-sc
	    stream.flatMap { _.tryConsume { input =>
	        Iterator
            .continually (input.read)
            .takeWhile { _ != -1 }
            .foreach (output.write)
		} }
	}
}
