package utopia.vault.nosql.view

import utopia.vault.nosql.template.Filterable
import utopia.vault.sql.Condition

/**
  * A common trait for access points whose conditions may be enhanced to cover a more precise area
  * @tparam Sub Type of the (filtered) views constructed
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait FilterableView[+Sub] extends View with ViewFactory[Sub] with Filterable[Sub]
{
	// IMPLEMENTED  ----------------
	
	override def filter(condition: Condition): Sub = apply(mergeCondition(condition))
	
	
	// OTHER    --------------------
	
	/**
	 * @param alternativeCondition A condition alternative to the current access condition
	 * @return A copy of this access extended to cover the specified condition, also
	 */
	def orWhere(alternativeCondition: Condition) = accessCondition match {
		case Some(originalCondition) => apply(originalCondition || alternativeCondition)
		case None => self
	}
}
