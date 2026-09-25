package utopia.flow.parse

import utopia.flow.parse.AutoClose._

import java.io.{BufferedInputStream, BufferedOutputStream, InputStream, OutputStream, OutputStreamWriter}
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
		// COMPUTED ----------------------
		
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
		
		
		// OTHER    ----------------------
		
		/**
		 * @param bufferSize Buffer size to apply, if constructing a buffered input-stream
		 * @return A buffered version of this stream. This stream if already buffered.
		 */
		def bufferedWithSize(bufferSize: => Int) = stream match {
			case s: BufferedInputStream => s
			case s => new BufferedInputStream(s, bufferSize)
		}
		
		/**
		 * Writes the contents of this stream into an output stream. Uses a separate buffer.
		 * @param output An output stream to write to
		 * @param bufferSize Size of the external buffer used, in bytes. Default = 1024.
		 */
		def writeTo(output: OutputStream, bufferSize: Int = 1024) = {
			val buffer = new Array[Byte](bufferSize)
			Iterator
				.continually { stream.read(buffer, 0, bufferSize) }
				.takeWhile { _ != -1 }
				.foreach { _ => output.write(buffer, 0, bufferSize) }
		}
	}
	
	implicit class RichOutputStream(val stream: OutputStream) extends AnyVal
	{
		/**
		 * @return A buffered copy of this stream. Yields this stream if already buffered.
		 */
		def buffered = stream match {
			case s: BufferedOutputStream => s
			case s => new BufferedOutputStream(s)
		}
		
		/**
		 * Writes to this stream using a [[BufferedPrintWriter]].
		 *
		 * Note: This stream is closed once the specified function finishes.
		 *
		 * @param f A function that receives a print writer for writing into this stream
		 * @param codec Implicit charset information used
		 * @tparam A Type of 'f' results
		 * @return Results of 'f'. Throws if 'f' throws, or if writer-construction fails.
		 */
		def writeUsing[A](f: BufferedPrintWriter => A)(implicit codec: Codec): A = writeUsing(codec.charSet)(f)
		/**
		 * Writes to this stream using a [[BufferedPrintWriter]].
		 *
		 * Note: This stream is closed once the specified function finishes.
		 *
		 * @param charset Character-set to use
		 * @param bufferSize Applied buffer size. Default = 8M.
		 * @param autoFlush Whether to automatically flush this stream whenever a new line is printed.
		 *                  Default = false.
		 * @param f A function that receives a print writer for writing into this stream
		 * @tparam A Type of 'f' results
		 * @return Results of 'f'. Throws if 'f' throws, or if writer-construction fails.
		 */
		def writeUsing[A](charset: Charset, bufferSize: Int = BufferedPrintWriter.defaultBufferSize,
		                  autoFlush: Boolean = false)
		                 (f: BufferedPrintWriter => A) =
			stream.consume { stream =>
				new OutputStreamWriter(stream, charset)
					.consume { new BufferedPrintWriter(_, bufferSize, autoFlush).consume(f) }
			}
		
		/**
		 * Opens a new writer to this stream. May throw.
		 * @param bufferSize Buffer size to apply. Default = 8M.
		 * @param autoFlush Whether to automatically flush this writer whenever println is called. Default = false.
		 * @param codec Implicit character encoding used.
		 * @return A new open writer to this stream.
		 */
		def openWriter(bufferSize: Int = 8192, autoFlush: Boolean = false)(implicit codec: Codec) =
			new BufferedPrintWriter(new OutputStreamWriter(stream, codec.charSet), bufferSize, autoFlush)
	}
}
