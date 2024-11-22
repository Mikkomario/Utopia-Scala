package utopia.flow.parse

import AutoClose._
import java.io.{BufferedInputStream, InputStream}

/**
  * Provides additional functions for streams
  * @author Mikko Hilpinen
  * @since 22.11.2024, v2.5.1
  */
object StreamExtensions
{
	implicit class RichInputStream(val s: InputStream) extends AnyVal
	{
		/**
		  * @return This stream as a buffered input stream.
		  *         Yields this stream if this is already buffered.
		  */
		def buffered = s match {
			case s: BufferedInputStream => s
			case s => new BufferedInputStream(s)
		}
		
		/**
		  * Checks whether this stream is empty.
		  * @return None if this stream was empty, in which case this stream was also closed.
		  *         Some if this stream contains at least 1 byte.
		  */
		def notEmpty = {
			val b = buffered
			b.mark(1)  // Marks the current position with a 1-byte read limit
			
			// Case: Empty stream => Yields None
			if (b.read() == -1) {
				b.closeQuietly()
				None
			}
			// Case: Non-empty stream => Moves back to the beginning and yields the non-empty stream
			else {
				b.reset()
				Some(buffered)
			}
		}
	}
}
