package utopia.flow.util

import utopia.flow.async.ShutdownReaction.SkipDelay
import utopia.flow.async.{Process, ProcessState, Volatile, VolatileOption, Wait}
import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.AutoClose._
import utopia.flow.util.FileExtensions._

import java.io.{FileOutputStream, OutputStream, OutputStreamWriter, PrintWriter}
import java.nio.file.Path
import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.util.{Success, Try}

object KeptOpenWriter
{
	/**
	  * Creates a new writer wrapper
	  * @param path Path to the file that is modified
	  * @param keepOpenDuration Duration how long the file / writer is kept open after each write
	  * @param codec Implicit encoding to use
	  * @param exc Implicit execution context used for closing the writer asynchronously
	  * @return A new writer wrapper
	  */
	def apply(path: Path, keepOpenDuration: FiniteDuration)
	         (implicit codec: Codec, exc: ExecutionContext) =
	{
		val existingPathPointer = Lazy { path.createDirectories() }
		new KeptOpenWriter(keepOpenDuration)(new FileOutputStream(existingPathPointer.value.get.toFile, true))
	}
}

/**
  * This wrapper opens and closes the underlying writer as necessary. The wrapped writer is kept open for a season
  * in case multiple write calls are made sequentially.
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class KeptOpenWriter(keepOpenDuration: FiniteDuration)(generate: => OutputStream)
                    (implicit codec: Codec, exc: ExecutionContext)
{
	// ATTRIBUTES   ----------------------------
	
	private val lastAccessPointer = Volatile(Instant.EPOCH)
	private val writerPointer = VolatileOption[(PrintWriter, Future[ProcessState])]()
	
	
	// COMPUTED --------------------------------
	
	private def lastAccessTime = lastAccessPointer.value
	private def lastAccessTime_=(newTime: Instant) = lastAccessPointer.value = newTime
	
	
	// OTHER    --------------------------------
	
	/**
	  * Performs a write operation
	  * @param f A function that uses a writer (any thrown errors are catched)
	  * @tparam U Function result type
	  * @return Success if writing (including function call) succeeded, failure otherwise
	  */
	def apply[U](f: PrintWriter => U) = {
		lastAccessTime = Now
		writerPointer.pop { existing =>
			// Uses a pre-created writer, if possible
			val writer = existing match {
				case Some(e) => Success(e)
				case None =>
					// Opens a new print writer. On failure closes the underlying stream and other assets.
					Try {
						val stream = generate
						val writer = Try { new OutputStreamWriter(stream, codec.charSet) }.flatMap { writer =>
							val printWriter = Try { new PrintWriter(writer) }
							if (printWriter.isFailure)
								writer.closeQuietly()
							printWriter
						}
						if (writer.isFailure)
							stream.closeQuietly()
						writer
					}.flatten.map { writer =>
						// Schedules an automated closing for the writer
						val waitLock = new AnyRef
						val closeProcess = Process(waitLock, SkipDelay) { hurryPointer =>
							// Waits until the writer has been unused long enough
							while (!hurryPointer.value && Now < (lastAccessTime + keepOpenDuration)) {
								Wait(lastAccessTime + keepOpenDuration, waitLock)
							}
							// Closes the writer
							writerPointer.update { _ =>
								writer.closeQuietly()
								None
							}
						}
						closeProcess.runAsync()
						writer -> closeProcess.completionFuture
					}
			}
			// Attempts to perform the writing operation
			val result = writer.flatMap { case (writer, _) => Try { f(writer) } }
			// Updates writer state and returns success or failure
			result -> writer.toOption
		}
	}
}
