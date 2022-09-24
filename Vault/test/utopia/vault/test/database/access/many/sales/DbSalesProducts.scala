package utopia.vault.test.database.access.many.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple sales products at a time
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object DbSalesProducts extends ManySalesProductsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted sales products
	  * @return An access point to sales products with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbSalesProductsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbSalesProductsSubset(targetIds: Set[Int]) extends ManySalesProductsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

