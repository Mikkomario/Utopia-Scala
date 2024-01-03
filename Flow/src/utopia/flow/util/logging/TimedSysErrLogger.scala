package utopia.flow.util.logging

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.TimedSysErrLogger.timeFormat
import utopia.flow.util.StringExtensions._

import java.time.format.DateTimeFormatter
import scala.annotation.unused
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object TimedSysErrLogger
{
	// ATTRIBUTES   --------------------
	
	private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
	
	private lazy val commonInstance = apply(5.seconds)
	
	
	// IMPLICIT -----------------------
	
	// Implicitly converts the companion object to a shared instance
	implicit def objectToInstance(@unused o: TimedSysErrLogger.type): TimedSysErrLogger = commonInstance
	
	
	// OTHER    -----------------------
	
	/**
	  * Creates a new logger instance
	  * @param bundleDuration Duration during which consecutive logging entries (without Throwables)
	  *                       should be bundled together.
	  * @return A new logger instance
	  */
	def apply(bundleDuration: Duration) = new TimedSysErrLogger(bundleDuration)
}

/**
  * Used for simple logging to System.err. Includes time information.
  * @author Mikko Hilpinen
  * @since 03/01/2024, v2.3
  */
class TimedSysErrLogger(bundleDuration: Duration) extends Logger
{
	// ATTRIBUTES   ---------------------
	
	private var lastLogTime = Now.toLocalDateTime
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(error: Option[Throwable], message: String): Unit = {
		// Determines whether a new separated group of entries should be started,
		// or whether to append the previous list of entries
		val t = Now.toLocalDateTime
		val startsNewGroup = error.isDefined || bundleDuration.finite.exists { t > lastLogTime + _ }
		
		// Writes the header, if appropriate
		if (startsNewGroup)
			System.err.println(s"\n${timeFormat.format(t)}${message.prependIfNotEmpty(": ")}")
		else if (message.nonEmpty)
			System.err.println(s"$message (${ (t - lastLogTime).description })")
		
		lastLogTime = t
		
		// Prints the stack trace, if appropriate
		error.foreach { _.printStackTrace() }
	}
}
