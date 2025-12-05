package utopia.access.model.header

import utopia.flow.util.Mutate

/**
 * Common trait for header values that may be mapped
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
trait MappableHeaderValue[A, +Repr] extends HeaderValue[A]
{
	/**
	 * @param f A mapping function applied to the parsed representation of this header value
	 * @return A mapped copy of this value
	 */
	def map(f: Mutate[A]): Repr
}
