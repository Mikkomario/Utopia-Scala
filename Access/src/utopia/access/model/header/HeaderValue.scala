package utopia.access.model.header

import utopia.access.model.header.HeaderValue.LazyHeaderValue
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.caching.Lazy

object HeaderValue
{
	// COMPUTED -------------------------
	
	def empty = EmptyHeaderValue
	
	
	// OTHER    -------------------------
	
	def apply(text: String): TextHeaderValue = new TextHeaderValue(text)
	def apply[A](text: String, parsed: A)(implicit valueConversion: A => ValueConvertible): HeaderValue[A] =
		new _HeaderValue[A](text, parsed, v => valueConversion(v).toValue)
	
	
	// NESTED   -------------------------
	
	private class LazyHeaderValue[+A](lazyParsed: Lazy[A], toText: A => String, convert: A => Value)
		extends HeaderValue[A]
	{
		// ATTRIBUTES   -------------------
		
		private val lazyText = lazyParsed.map(toText)
		private val lazyValue = lazyParsed.map(convert)
		
		
		// IMPLEMENTED  -------------------
		
		override def text: String = lazyText.value
		override def parsed: A = lazyParsed.value
		override def toValue: Value = lazyValue.value
	}
	
	private class _HeaderValue[+A](override val text: String, override val parsed: A, convert: A => Value)
		extends HeaderValue[A]
	{
		override lazy val toValue: Value = convert(parsed)
	}
}

/**
 * Represents a (parsed) header value
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
trait HeaderValue[+A] extends ValueConvertible with EqualsBy
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The full text included in this header value
	 */
	def text: String
	/**
	 * @return The parsed value wrapped by this header value
	 */
	def parsed: A
	
	
	// IMPLEMENTED  --------------------
	
	override def toString: String = text
	
	override protected def equalsProperties: IterableOnce[Any] = Single(text)
	
	
	// OTHER    -----------------------
	
	def mapText(f: Mutate[String]) = new TextHeaderValue(f(text))
	
	def mapParsed[B](f: A => B)(toText: B => String)(implicit toValue: B => ValueConvertible): HeaderValue[B] =
		new LazyHeaderValue[B](Lazy { f(parsed) }, toText, v => toValue(v).toValue)
}
