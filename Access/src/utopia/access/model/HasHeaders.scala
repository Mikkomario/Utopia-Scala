package utopia.access.model

/**
 * Common trait for items, which specify http headers
 * @author Mikko Hilpinen
 * @since 05.11.2025, v1.6.2
 */
trait HasHeaders
{
	// ABSTRACT ----------------------
	
	/**
	 * @return The headers describing this item
	 */
	def headers: Headers
}
