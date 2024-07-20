package utopia.flow.parse

import scala.util.Try

/**
  * This object offers utility tools for working with closeable resources
  * @author Mikko Hilpinen
  * @since 12.5.2018
  * */
object AutoClose
{
	/**
	  * Uses a resource and then closes it. May throw.
	  */
	def apply[T <: AutoCloseable, B](closeable: T)(f: T => B) = closeable.consume(f)
	
	/**
	  * Uses a resource and then closes it. Will not throw.
	  */
	def tryAndClose[T <: AutoCloseable, B](closeable: T)(f: T => B) = closeable.tryConsume(f)
	
	/**
	  * This class allows closeable entities be handled and closed in a safe manner
	  */
	implicit class ExtendedCloseable[T <: AutoCloseable](val c: T) extends AnyVal
	{
		/**
		  * Consumes this entity, then closes. Wraps the results in a try
		  */
		def tryConsume[B](f: T => B) = Try(consume(f))
		
		/**
		  * Consumes this entity, then closes. May throw if the provided function throws
		  */
		def consume[B](f: T => B): B = {
			try {
				f(c)
			}
			finally {
				Try(c.close())
			}
		}
		
		/**
		  * Closes this item without a chance of throwing an exception
		  */
		def closeQuietly() = Try { c.close() }
	}
}
