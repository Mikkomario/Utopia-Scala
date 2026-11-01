package utopia.vault.store

import utopia.flow.util.Mutate

/**
 * Common trait for classes which provide copy functions based on a "data" instance's properties.
 * @tparam Data Type of the wrapped data instance
 * @tparam Repr Type of the implementing class
 * @author Mikko Hilpinen
 * @since 01.11.2026, v2.2
 */
trait CanCopyData[Data, +Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Wrapped data
	 */
	def data: Data
	/**
	 * @param data New data instance to wrap
	 * @return A copy of this item wrapping the specified data
	 */
	def wrap(data: Data): Repr
	
	
	// OTHER    ------------------------
	
	/**
	 * @param f A mapping function to apply to the wrapped data instance
	 * @return A copy of this instance wrapping the modified data
	 */
	def mapData(f: Mutate[Data]): Repr = wrap(f(data))
}
