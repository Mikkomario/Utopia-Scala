package utopia.disciple.http.request

import utopia.access.http.ContentType
import utopia.flow.parse.AutoClose._

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}
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
	 * The length of the body content in bytes, if applicable
	 */
	def contentLength: Option[Long]
	/**
	 * The content type of this body
	 */
	def contentType: ContentType
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
	 * Writes the contents of this body's stream into an output stream. Applies buffering.
	 */
	def writeTo(output: OutputStream) = {
	    // See: https://stackoverflow.com/questions/6927873/
	    // how-can-i-read-a-file-to-an-inputstream-then-write-it-into-an-outputstream-in-sc
	    stream.flatMap {
		    _.tryConsume { input =>
			    // Checks whether buffering is applied within the streams, or whether it should be applied separately
			    // Case: The streams already apply buffering => Processes byte-by-byte
			    if (input.isInstanceOf[BufferedInputStream] && output.isInstanceOf[BufferedOutputStream]) {
				    Iterator
					    .continually { input.read() }
					    .takeWhile { _ != -1 }
					    .foreach(output.write)
			    }
			    // Case: Both streams don't apply buffering => Processes using a separate buffer
			    else {
				    val buffer = new Array[Byte](1024)
				    Iterator
					    .continually { input.read(buffer) }
					    .takeWhile { _ != -1 }
					    .foreach { _ => output.write(buffer) }
			    }
			}
	    }
	}
}
