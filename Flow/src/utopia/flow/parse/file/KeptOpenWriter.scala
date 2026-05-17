package utopia.flow.parse.file

import utopia.flow.async.context.Scheduler
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.Duration
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.caching.{ClosesAfterIdle, Lazy}

import java.io.{FileOutputStream, OutputStream, OutputStreamWriter, PrintWriter}
import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.io.Codec
import scala.util.{Failure, Success, Try}

object KeptOpenWriter
{
	/**
	  * Creates a new writer wrapper
	  * @param path Path to the file that is modified
	  * @param keepOpenDuration Duration how long the file / writer is kept open after each write
	  * @param rethrows Whether errors thrown by the writer functions should be rethrown
	 *                 (default = false = errors are caught)
	 * @param logsFailures Whether all encountered writing-failures should be automatically logged using 'logger'
	 * @param codec Implicit encoding to use
	  * @param exc Implicit execution context used for closing the writer asynchronously
	  * @param logger Logging implementation used
	 * @return A new writer wrapper
	  */
	def apply(path: Path, keepOpenDuration: Duration, rethrows: Boolean = false, logsFailures: Boolean = false)
	         (implicit codec: Codec, exc: ExecutionContext, scheduler: Scheduler, logger: Logger) =
	{
		val existingPathPointer = Lazy { path.createDirectories() }
		new KeptOpenWriter(keepOpenDuration, rethrows, logsFailures)(
			new FileOutputStream(existingPathPointer.value.get.toFile, true))
	}
}

/**
  * This wrapper opens and closes the underlying writer as necessary. The wrapped writer is kept open for a season
  * in case multiple write calls are made sequentially.
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class KeptOpenWriter(keepOpenDuration: Duration, rethrows: Boolean = false, logsFailures: Boolean = false)
                    (generate: => OutputStream)
                    (implicit codec: Codec, exc: ExecutionContext, scheduler: Scheduler, log: Logger)
{
	// ATTRIBUTES   ----------------------------
	
	private val writerP = ClosesAfterIdle.closingOnJvmShutdown.after(keepOpenDuration).trying {
		// Opens a new print writer. On failure closes the underlying stream and other assets.
		Try
			.apply {
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
			}
			.flatten
	}
	
	
	// OTHER    --------------------------------
	
	/**
	  * Performs a write operation
	  * @param f A function that uses a writer
	  * @tparam A Function result type
	  * @return Success if writing succeeded, failure otherwise
	  */
	def apply[A](f: PrintWriter => A) = {
		val result = writerP.keepOpenDuring {
			case Success(writer) =>
				if (rethrows)
					Success(f(writer))
				else
					Try { f(writer) }
			
			case Failure(error) => Failure(error)
		}
		if (logsFailures)
			result.logWithMessage("Failure while writing")
		result
	}
}
