package utopia.logos.model.template

/**
 * Common trait for factory classes that generate instances based on their placing / location
 * @author Mikko Hilpinen
 * @since 14/03/2024, v0.2
 */
trait PlacedFactory[+A]
{
	// ABSTRACT ----------------------
	
	/**
	 * @param orderIndex 0-based index that determines the location of this instance within the applicable context
	 * @return A (copy of this) item assigned to the specified location
	 */
	def at(orderIndex: Int): A
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return An item placed at the beginning of its context
	 */
	def first = at(0)
}
