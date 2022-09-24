package utopia.vault.test.database.access.many.sales

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple purchases at a time
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object DbPurchases extends ManyPurchasesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted purchases
	  * @return An access point to purchases with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbPurchasesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbPurchasesSubset(targetIds: Set[Int]) extends ManyPurchasesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

