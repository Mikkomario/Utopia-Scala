package utopia.flow.error

import utopia.flow.collection.mutable.iterator.OptionsIterator

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
		def stackTraceString = {
			val writer = new StringWriter()
			e.printStackTrace(new PrintWriter(writer))
			writer.toString
		}
		
		/**
		  * @return An iterator that returns the underlying causes of this throwable (from top to root)
		  */
		def causesIterator =
			OptionsIterator.iterate(Option(e.getCause)) { t => Option(t.getCause) }
	}
}
