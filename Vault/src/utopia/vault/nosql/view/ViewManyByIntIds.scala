package utopia.vault.nosql.view

import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

/**
  * Common trait for view factories that provide views to multiple items at once
  * and support integer index -based targeting
  * @tparam V Type of the views constructed
  * @author Mikko Hilpinen
  * @since 30.07.2024, v1.20
  */
trait ViewManyByIntIds[+V] extends ViewFactory[V] with Indexed
{
	/**
	  * @param ids Ids of the targeted items
	  * @return Access to those items
	  */
	def apply(ids: IterableOnce[Int]): V = apply(Condition.indexIn(index, ids))
}
