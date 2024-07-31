package utopia.vault.nosql.view

import utopia.vault.sql.Condition

/**
  * Common trait for factories that construct views based on filter conditions
  * @tparam V Type of view produced by this factory
  * @author Mikko Hilpinen
  * @since 30.07.2024, v1.20
  */
trait ViewFactory[+V]
{
	/**
	  * @param condition A search condition to apply
	  * @return View to items that fulfill that search condition
	  */
	def apply(condition: Condition): V
}
