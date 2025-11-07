package utopia.flow.parse

import AutoClose._

import java.io.{BufferedInputStream, InputStream, OutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.Charset
import scala.io.Codec

/**
  * Provides additional functions for streams
  * @author Mikko Hilpinen
  * @since 22.11.2024, v2.5.1
  */
object StreamExtensions
{
	implicit class RichInputStream(val stream: InputStream) extends AnyVal
	{
		/**
		  * @return This stream as a buffered input stream.
		  *         Yields this stream if this is already buffered.
		  */
		def buffered = stream match {
			case s: BufferedInputStream => s
			case s => new BufferedInputStream(s)
		}
		
		/**
		  * Checks whether this stream is empty.
		  * @return None if this stream was empty, in which case this stream was also closed.
		  *         Some if this stream contains at least 1 byte.
		  */
		def notEmpty: Option[BufferedInputStream] = {
			val _buffered = buffered
			_buffered.mark(1)  // Marks the current position with a 1-byte read limit
			
			// Case: Empty stream => Yields None
			if (_buffered.read() == -1) {
				_buffered.closeQuietly()
				None
			}
			// Case: Non-empty stream => Moves back to the beginning and yields the non-empty stream
			else {
				_buffered.reset()
				Some(_buffered)
			}
		}
	}
	
	implicit class RichOutputStream(val stream: OutputStream) extends AnyVal
	{
		/**
		 * Writes to this stream using a [[PrintWriter]].
		 *
		 * Note: This stream is closed once the specified function finishes.
		 *
		 * @param f A function that receives a print writer for writing into this stream
		 * @param codec Implicit charset information used
		 * @tparam A Type of 'f' results
		 * @return Results of 'f'. Throws if 'f' throws, or if writer-construction fails.
		 */
		def writeUsing[A](f: PrintWriter => A)(implicit codec: Codec): A =
			writeUsing(codec.charSet)(f)
		/**
		 * Writes to this stream using a [[PrintWriter]].
		 *
		 * Note: This stream is closed once the specified function finishes.
		 *
		 * @param charset Character-set to use
		 * @param autoFlush Whether to automatically flush this stream whenever a new line is printed.
		 *                  Default = false.
		 * @param f A function that receives a print writer for writing into this stream
		 * @tparam A Type of 'f' results
		 * @return Results of 'f'. Throws if 'f' throws, or if writer-construction fails.
		 */
		def writeUsing[A](charset: Charset, autoFlush: Boolean = false)(f: PrintWriter => A) =
			stream.consume { stream =>
				new OutputStreamWriter(stream, charset).consume { writer =>
					new PrintWriter(writer, autoFlush).consume(f)
				}
			}
	}
}
