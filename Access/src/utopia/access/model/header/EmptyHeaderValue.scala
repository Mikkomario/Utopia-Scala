package utopia.access.model.header

import utopia.flow.generic.model.immutable.Value

object EmptyHeaderValue
{
	// ATTRIBUTES   -----------------
	
	lazy val text = apply("")
	lazy val value = apply(Value.empty)
	
	
	// OTHER    ---------------------
	
	def apply[A](parsed: A): EmptyHeaderValue[A] = new _EmptyHeaderValue[A](parsed)
	
	
	// NESTED   ---------------------
	
	private class _EmptyHeaderValue[+A](override val parsed: A) extends EmptyHeaderValue[A]
}

/**
 * Common trait for empty header values
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
trait EmptyHeaderValue[+A] extends HeaderValue[A]
{
	override def text: String = ""
	override def toValue: Value = Value.empty
}
