package utopia.vault.nosql.view

import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Condition, ConditionElement}

/**
 * Common trait for views that use an integer-based index for in & excluding conditions
 * @author Mikko Hilpinen
 * @since 29.10.2025, v2.0
 */
trait IntIndexFilterableView[+Repr] extends FilterableView[Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return An integer type column used in "in" and "excluding" conditions
	 */
	protected def idColumn: Column
	
	
	// IMPLEMENTED  -----------------------
	
	/**
	 * @param indices Targeted indices
	 * @return Copy of this access limited to the specified indices
	 */
	def in(indices: IterableOnce[Int]) = filter(Condition.indexIn(idColumn, indices))
	/**
	 * @param indices Indices to exclude
	 * @return Copy of this access excluding the specified indices
	 */
	def excluding(indices: IterableOnce[Int]) = filter(Condition.indexNotIn(idColumn, indices))
	/**
	 * @param index Index to exclude
	 * @return Copy of this access excluding the specified index
	 */
	def excluding(index: ConditionElement) = filter(idColumn <> index)
}
