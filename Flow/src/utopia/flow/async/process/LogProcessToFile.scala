package utopia.flow.async.process

import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.sys.process.ProcessLogger

object LogProcessToFile
{
	/**
	 * @param out Primary output file
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation used on file-writing failures
	 * @return A factory for constructing the logger
	 */
	def apply(out: Path)(implicit exc: ExecutionContext, log: Logger) = LogProcessToFileFactory(out, None, 15.seconds)
	
	
	// NESTED   ------------------------
	
	case class LogProcessToFileFactory(out: Path, err: Option[Path], keepOpenDuration: Duration)
	                                  (implicit exc: ExecutionContext, log: Logger)
	{
		/**
		 * @param err Path to where error records should be written
		 * @return Copy of this factory with the specified error path
		 */
		def withErr(err: Path) = copy(err = Some(err))
		/**
		 * @param duration Duration how long the underlying writer(s) should be kept open
		 * @return Copy of this factory with the specified setting
		 */
		def keptOpenFor(duration: Duration) = copy(keepOpenDuration = duration)
		
		/**
		 * @return A new logger
		 */
		def apply() = err.filterNot { _ == out } match {
			case Some(err) => new LogProcessToFile(Pair(out, err).map { KeptOpenWriter(_, keepOpenDuration) })
			// Case: Logging both standard output and error output to the same file
			case None => new LogProcessToFile(Pair.twice(KeptOpenWriter(out, keepOpenDuration)))
		}
	}
}

/**
 * A ProcessLogger implementation that writes to local file or files
 * @author Mikko Hilpinen
 * @since 05.03.2026, v2.8
 */
class LogProcessToFile(writers: Pair[KeptOpenWriter]) extends ProcessLogger
{
	override def out(s: => String): Unit = writers.first { _.println(s) }
	override def err(s: => String): Unit = writers.second { _.println(s) }
	
	override def buffer[T](f: => T): T = f
}
