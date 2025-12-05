package utopia.access.model.header

import utopia.access.model.header.HeaderValue.EmptyHeaderValue
import utopia.flow.generic.model.template.ValueConvertible

object Header
{
	// OTHER    -------------------------
	
	def empty[A](name: String, parsed: A): Header[A] = apply(name, EmptyHeaderValue(parsed))
	
	def apply(name: String, text: String): Header[String] = apply(name, HeaderValue(text))
	def apply[A](name: String, value: HeaderValue[A]): Header[A] = new _Header[A](name, value)
	
	def parsed[A](name: String, text: String, parsed: A)(implicit toValue: A => ValueConvertible): Header[A] =
		apply(name, HeaderValue(text, parsed))
	
	
	// NESTED   -------------------------
	
	private class _Header[+A](override val name: String, override val value: HeaderValue[A]) extends Header[A]
}

/**
 * Represents an individual HTTP header
 * @tparam A Type of this header's parsed value
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
trait Header[+A] extends HeaderLike[A, HeaderValue[A]]
{
	// OTHER    ------------------------
	
	/**
	 * Maps the parsed value of this header
	 * @param f A mapping function applied to this header's value
	 * @tparam B Type of the modified value
	 * @return A mapped copy of this header
	 */
	def map[B](f: HeaderValue[A] => HeaderValue[B]): Header[B] = Header(name, f(value))
}
