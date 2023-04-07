package utopia.flow.util.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.error.ErrorExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today}
import utopia.flow.util.logging.FileLogger.dateTimeFormat
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.caching.DeprecatingLazy

import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.io.Codec

object FileLogger
{
	private val dateTimeFormat = DateTimeFormatter.ofPattern("hh:mm:ss")
}

/**
 * A logger implementation that writes to a file
 * @author Mikko Hilpinen
 * @since 24.7.2022, v1.16
  * @param dir Directory where new log files will be generated
  * @param groupDuration Duration within which the log entries will be grouped together (default = 0 = no grouping)
  * @param copyToSysErr Whether log entries should be copied to System.err (default = false)
 */
class FileLogger(private var dir: Path = "log", groupDuration: Duration = Duration.Zero, copyToSysErr: Boolean = false)
                (implicit codec: Codec, exc: ExecutionContext)
	extends Logger
{
	// ATTRIBUTES   --------------------------
	
	private val lastLogTimePointer = Pointer(Instant.EPOCH)
	
	// Changes the targeted file each day
	private val writerPointer = DeprecatingLazy {
		val date = Today.toLocalDate
		KeptOpenWriter(dir/s"$date.txt", 10.seconds) -> date
	} { _._2 == Today.toLocalDate }
	
	
	// COMPUTED -----------------------------
	
	private implicit def backupLogger: Logger = SysErrLogger
	
	/**
	 * @return The directory where log files are added
	 */
	def directory = dir
	def directory_=(newLogDirectory: Path) = {
		dir = newLogDirectory
		writerPointer.reset()
	}
	
	private def writer = writerPointer.value._1
	
	
	// IMPLEMENTED    -----------------------------
	
	override def apply(error: Option[Throwable], message: String) = {
		// The header is different for consecutive messages
		val header = {
			val now = Now.toInstant
			val lastLogTime = lastLogTimePointer.getAndSet(now)
			if (now - lastLogTime < groupDuration)
				message
			else
				s"\n${now.toLocalTime.format(dateTimeFormat)}: $message"
		}
		// Prints to the file
		val fileWriteResult = writer { w =>
			w.println(header)
			error.foreach { _.printStackTrace(w) }
		}
		// May print to sysErr also
		if (fileWriteResult.isFailure || copyToSysErr) {
			printErr(header)
			error.foreach { e => printErr(e.stackTraceString) }
		}
		// Also records writing errors
		fileWriteResult.failure.foreach { error =>
			printErr("Logging failed")
			error.printStackTrace()
		}
	}
	
	private def printErr(message: String) = System.err.println(message)
}
