package utopia.access.model

import utopia.flow.util.Mutate

/**
 * Common trait for copyable items that have http headers
 * @author Mikko Hilpinen
 * @since 05.11.2025, v1.6.2
 */
trait Headered[+Repr] extends HasHeaders
{
	// ABSTRACT ----------------------
	
	/**
	 * @param headers Headers for this item
	 * @param overwrite Whether to overwrite the current headers (true),
	 *                  or to append to the current headers (false, default)
	 * @return Copy of this item with modified headers
	 */
	def withHeaders(headers: Headers, overwrite: Boolean = false): Repr
	/**
	 * @param f A mapping function applied to this item's headers
	 * @return Copy of this item with modified headers
	 */
	def mapHeaders(f: Mutate[Headers]) = withHeaders(f(headers), overwrite = true)
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return Copy of this item without any headers
	 */
	def withoutHeaders = withHeaders(Headers.empty, overwrite = true)
}
