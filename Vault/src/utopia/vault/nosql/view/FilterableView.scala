package utopia.vault.nosql.view

import utopia.vault.model.error.ColumnNotFoundException
import utopia.vault.sql.{Condition, ConditionElement}

/**
  * A common trait for access points whose conditions may be enhanced to cover a more precise area
  * @tparam Sub Type of the (filtered) views constructed
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait FilterableView[+Sub] extends View with ViewFactory[Sub]
{
	// ABSTRACT	----------------------
	
	/**
	  * @return This view
	  */
	protected def self: Sub
	
	
	// OTHER    --------------------
	
	/**
	 * Applies a filter over this access point
	 * @param additionalCondition An additional search condition applied
	 * @return A copy of this access which only includes the sub-set of items which fulfill the specified condition
	 *         (as well as other conditions possibly defined by this view)
	 */
	def filter(additionalCondition: Condition): Sub = apply(mergeCondition(additionalCondition))
	/**
	  * @param additionalCondition An additional search condition to apply
	  *                            (None if no additional condition should be applied)
	  * @return A filtered copy of this access point.
	  *         I.e. an access point that applies the specified condition to all queries.
	  */
	def filter(additionalCondition: Option[Condition]): Sub = additionalCondition match {
		case Some(c) => filter(c)
		case None => self
	}
	
	/**
	 * @param alternativeCondition A condition alternative to the current access condition
	 * @return A copy of this access extended to cover the specified condition, also
	 */
	def orWhere(alternativeCondition: Condition) = accessCondition match {
		case Some(originalCondition) => apply(originalCondition || alternativeCondition)
		case None => self
	}
	
	/**
	 * @param indices Targeted indices
	 * @return Copy of this access limited to the specified indices
	 */
	@throws[ColumnNotFoundException]("If the primary table does not specify a primary key column")
	def in(indices: IterableOnce[Int]) = filter(Condition.indexIn(table.getPrimaryKey, indices))
	/**
	 * @param indices Indices to exclude
	 * @return Copy of this access excluding the specified indices
	 */
	@throws[ColumnNotFoundException]("If the primary table does not specify a primary key column")
	def excluding(indices: IterableOnce[Int]) = filter(Condition.indexNotIn(table.getPrimaryKey, indices))
	/**
	 * @param index Index to exclude
	 * @return Copy of this access excluding the specified index
	 */
	@throws[ColumnNotFoundException]("If the primary table does not specify a primary key column")
	def excluding(index: ConditionElement) = filter(table.getPrimaryKey <> index)
}
