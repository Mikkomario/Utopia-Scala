package utopia.flow.error

import java.io.{PrintWriter, StringWriter}

/**
  * Extension methods for throwables / errors
  * @author Mikko Hilpinen
  * @since 6.6.2021, v1.10
  */
object ErrorExtensions
{
	implicit class RichThrowable(val e: Throwable) extends AnyVal
	{
		/**
		  * @return The stack trace of this throwable item as a string
		  */
		def stackTraceString =
		{
			val writer = new StringWriter()
			e.printStackTrace(new PrintWriter(writer))
			writer.toString
		}
	}
}
