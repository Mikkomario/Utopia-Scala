package utopia.flow.util

import utopia.flow.util.AutoClose._

import scala.util.Try

object AutoCloseWrapper
{
	/**
	 * Creates a new wrapper
	 * @param content Wrapped item
	 * @param close A function for closing the item
	 * @tparam A Type of item
	 * @return Wrapped item
	 */
	def apply[A](content: A)(close: A => Unit) = new AutoCloseWrapper[A](content, close)
}

/**
 * Used for providing AutoCloseable features to classes implement close() but don't implement AutoCloseable
 * @author Mikko Hilpinen
 * @since 16.2.2020, v1.6.1
 */
class AutoCloseWrapper[A](val content: A, closeContent: A => Unit) extends AutoCloseable with Extender[A]
{
	// COMPUTED	---------------------------
	
	/**
	 * @return The contents of this wrapper (same as calling content)
	 */
	@deprecated("Please call .content or .wrapped instead", "v1.12.2")
	def get = content
	
	
	// IMPLEMENTED	-----------------------
	
	override def wrapped = content
	
	override def close() = closeContent(content)
	
	
	// OTHER	---------------------------
	
	/**
	 * Uses the contents of this wrapper in a function, then closes the contents
	 * @param f A function that consumes the contents of this wrapper
	 * @tparam B Result type
	 * @return Function result
	 */
	def consumeContent[B](f: A => B) = this.consume { wrap => f(wrap.content) }
	
	/**
	 * Uses the contents of this wrapper in a function, then closes the contents. Caches exceptions.
	 * @param f A function that consumes the contents of this wrapper
	 * @tparam B Result type
	 * @return Function result. Failure if an error was thrown during function call or closing.
	 */
	def tryConsumeContent[B](f: A => B) = Try { consumeContent(f) }
}
