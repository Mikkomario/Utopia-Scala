package utopia.echo.controller

import java.io.InputStream
import scala.annotation.tailrec

/**
  * An input stream modifier which replaces newline characters with whitespaces
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
// TODO: Probably remove this
class ReplaceNewlinesWithWhitespacesInputStream(input: InputStream, bufferSize: Int = 1024) extends InputStream
{
	// ATTRIBUTES   ----------------------
	
	private val buffer = new Array[Byte](bufferSize)
	private var offset = 0
	private var endOfStreamReached = false
	
	
	// IMPLEMENTED  ----------------------
	
	override def read() = {
		// Case: End of stream reached => Returns -1 = EOF
		if (endOfStreamReached && offset == 0)
			-1
		// Case: Stream not yet fully exhausted
		else {
		
		}
		???
	}
	
	
	// OTHER    -------------------------
	
	@tailrec
	private def fillBuffer(): Unit = {
		// Attempts to fill the buffer
		val bytesRead = input.read(buffer, offset, bufferSize - offset)
		// Updates the offset afterwards
		if (bytesRead > 0)
			offset += bytesRead
			
		// Case: End of input
		if (bytesRead == -1)
			endOfStreamReached = true
		// Case: There's still more room in the buffer => Continues reading
		else if (offset < bufferSize)
			fillBuffer()
	}
}
/*
class LineToSpaceInputStream(inputStream: InputStream) extends InputStream {
  private val bufferSize = 1024 // Adjust this size based on your needs
  private val buffer = new Array[Byte](bufferSize)
  private var offset = 0
  private var endOfStream = false

  override def read(): Int = {
    if (endOfStream && offset == 0) {
      -1 // Return EOF when the stream is exhausted
    } else {
      @tailrec
      def fillBuffer(): Unit = {
        val bytesRead = inputStream.read(buffer, offset, bufferSize - offset)
        if (bytesRead > 0) {
          offset += bytesRead
        }
        if (offset == bufferSize || bytesRead == -1) {
          endOfStream = bytesRead == -1
        } else {
          fillBuffer() // Recursively refill the buffer until we have enough or EOF
is reached
        }
      }
      fillBuffer()

      val result = if (offset > 0 && buffer(offset - 1) == '\n') {
        offset -= 1
        32 // Replace newline with space
      } else {
        buffer(offset - 1) & 0xFF
      }
      offset -= 1
      result
    }
  }
}

 */
