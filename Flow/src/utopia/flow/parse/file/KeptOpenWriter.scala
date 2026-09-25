package utopia.flow.parse.file

import utopia.flow.async.context.Scheduler
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.BufferedPrintWriter
import utopia.flow.parse.StreamExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.Duration
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.caching.{ClosesAfterIdle, Lazy}

import java.io.{OutputStream, OutputStreamWriter}
import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.io.Codec
import scala.util.{Failure, Success, Try}

object KeptOpenWriter
{
	// OTHER    ----------------------------
	
	/**
	 * @param keptOpenDuration Duration how long the stream / writer is kept open after each write.
	 * @param codec Implicit character encoding to use
	 * @param exc Implicit execution context used for closing the writer asynchronously
	 * @param scheduler Implicit scheduler for scheduling writer-close operations.
	 * @param logger Implicit logging implementation used
	 * @return A factory for constructing new writers
	 */
	def apply(keptOpenDuration: Duration)
	         (implicit codec: Codec, exc: ExecutionContext, scheduler: Scheduler, logger: Logger) =
		KeptOpenWriterFactory(keptOpenDuration)
	
	/**
	 * Creates a new writer wrapper
	 * @param path             Path to the file that is modified
	 * @param keepOpenDuration Duration how long the file / writer is kept open after each write
	 * @param rethrows         Whether errors thrown by the writer functions should be rethrown
	 *                         (default = false = errors are caught)
	 * @param logsFailures     Whether all encountered writing-failures should be automatically logged using 'logger'
	 * @param autoFlush        Whether to automatically flush the stream on every call to println. Default = false.
	 * @param codec            Implicit encoding to use
	 * @param exc              Implicit execution context used for closing the writer asynchronously
	 * @param logger           Logging implementation used
	 * @return A new writer wrapper
	 */
	@deprecated("Deprecated for removal. Please use .apply(Duration).to(Path) instead", "v2.9")
	def apply(path: Path, keepOpenDuration: Duration, rethrows: Boolean = false, logsFailures: Boolean = false,
	          autoFlush: Boolean = false)
	         (implicit codec: Codec, exc: ExecutionContext, scheduler: Scheduler, logger: Logger): KeptOpenWriter =
		apply(keepOpenDuration).copy(rethrows = rethrows, logs = logsFailures, autoFlushes = autoFlush).to(path)
	
	
	// NESTED   ----------------------------
	
	case class KeptOpenWriterFactory(duration: Duration, rethrows: Boolean = false, logs: Boolean = false,
	                                 autoFlushes: Boolean = false)
	                                (implicit codec: Codec, exc: ExecutionContext, scheduler: Scheduler, logger: Logger)
	{
		// COMPUTED ------------------------
		
		/**
		 * @return A copy of this factory that rethrows write-failures
		 */
		def throwing = copy(rethrows = true)
		/**
		 * @return A copy of this factory that logs encountered failures
		 */
		def logging = copy(logs = true)
		/**
		 * @return A copy of this factory that automatically flushes the underlying stream after every call to println.
		 */
		def autoFlushing = copy(autoFlushes = true)
		
		
		// OTHER    ------------------------
		
		/**
		 * @param path Path to write to
		 * @return A writer interface for that file
		 */
		def to(path: Path) = {
			val lazyExisting = Lazy { path.createDirectories() }
			using { lazyExisting.value.get.openOutputStream(append = true) }
		}
		/**
		 * Creates a new writer
		 * @param openStream A function that opens the stream to write to
		 * @return A writer interface
		 */
		def using(openStream: => OutputStream) = new KeptOpenWriter(duration, rethrows, logs, autoFlushes)(openStream)
	}
}

/**
  * This wrapper opens and closes the underlying writer as necessary. The wrapped writer is kept open for a season
  * in case multiple write calls are made sequentially.
 * @param keepOpenDuration Duration how long the file / writer is kept open after each write
 * @param rethrows Whether errors thrown by the writer functions should be rethrown
 *                 (default = false = errors are caught)
 * @param logsFailures Whether all encountered writing-failures should be automatically logged using 'logger'
 * @param autoFlush Whether to automatically flush the stream on every call to println. Default = false.
 * @param openStream A function that opens a new stream
 * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class KeptOpenWriter(keepOpenDuration: Duration, rethrows: Boolean = false, logsFailures: Boolean = false,
                     autoFlush: Boolean = false)
                    (openStream: => OutputStream)
                    (implicit codec: Codec, exc: ExecutionContext, scheduler: Scheduler, log: Logger)
{
	// ATTRIBUTES   ----------------------------
	
	private val writerP = ClosesAfterIdle.closingOnJvmShutdown.after(keepOpenDuration).trying {
		// Opens a new print writer. On failure closes the underlying stream and other assets.
		Try
			.apply {
				val stream = openStream
				val writer = Try { new OutputStreamWriter(stream.buffered, codec.charSet) }.flatMap { writer =>
					val printWriter = Try { new BufferedPrintWriter(writer, autoFlush = autoFlush) }
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
	  * Performs a write operation and flushes the writer / stream.
	  * @param f A function that uses a writer
	  * @tparam A Function result type
	  * @return Success if writing succeeded, failure otherwise
	  */
	def apply[A](f: BufferedPrintWriter => A) = {
		val result = writerP.keepOpenDuring {
			case Success(writer) =>
				if (rethrows) {
					val result = f(writer)
					writer.flush()
					Success(result)
				}
				else
					Try {
						val result = f(writer)
						writer.flush()
						result
					}
			case Failure(error) => Failure(error)
		}
		if (logsFailures)
			result.logWithMessage("Failure while writing")
		result
	}
}
