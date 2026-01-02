package utopia.vault.nosql.template

import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Condition, ConditionElement}

/**
 * Common trait for filterable elements that use an integer-based index in "in" & "excluding" -conditions
 * @author Mikko Hilpinen
 * @since 02.01.2026, v2.1
 */
trait IntIndexFilterable[+Repr] extends Filterable[Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return An integer type column used in "in" and "excluding" conditions
	 */
	protected def idColumn: Column
	
	
	// IMPLEMENTED  -----------------------
	
	/**
	 * @param id Targeted primary index / ID
	 * @return Access to that index
	 */
	def withId(id: ConditionElement) = filter(idColumn <=> id)
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
