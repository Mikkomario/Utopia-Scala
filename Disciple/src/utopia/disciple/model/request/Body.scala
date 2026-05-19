package utopia.disciple.model.request

import utopia.flow.parse.AutoClose._

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream}
import scala.util.Try

/**
* A body can be placed in an http request
* @author Mikko Hilpinen
* @since 1.5.2018
**/
@deprecated("Deprecated for removal. Please extend RequestBody instead", "v1.9.3")
trait Body extends RequestBody
{
    // ABSTRACT    -------------------------
	
	/**
	 * The content stream. May fail.
	 */
	def stream: Try[InputStream]
	
	
	// IMPLEMENTED  ------------------
	
	override def streaming: Boolean = !repeatable
	
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
				    // TODO: Optimize buffer size to match that of the outputStream, if applicable
				    val buffer = new Array[Byte](1024)
				    Iterator
					    .continually { input.read(buffer) }
					    .takeWhile { _ != -1 }
					    .foreach { _ => output.write(buffer) }
			    }
			}
	    }
	}
	
	override def close() = {
		// Consumes and closes the input stream
		stream.foreach { input =>
			val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
			Iterator.continually (input.read(bytes)).takeWhile { _ != -1 }.foreach { _ => () }
			input.close()
		}
	}
}
