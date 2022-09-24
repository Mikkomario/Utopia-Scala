package utopia.flow.util.logging

import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.error.ErrorExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.view.mutable.caching.DeprecatingLazy

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.io.Codec

/**
 * A logger implementation that writes to a file
 * @author Mikko Hilpinen
 * @since 24.7.2022, v0.1
 */
class FileLogger(private var dir: Path = "log", copyToSysErr: Boolean = false)
                (implicit codec: Codec, exc: ExecutionContext)
	extends Logger
{
	// ATTRIBUTES   --------------------------
	
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
		val header = s"${Now.toLocalTime}: $message"
		// Prints to the file
		val fileWriteResult = writer { w =>
			w.println(header)
			error.foreach { _.printStackTrace(w) }
			w.println()
		}
		// May print to sysErr also
		if (fileWriteResult.isFailure || copyToSysErr) {
			printErr("\n" + header)
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
