package utopia.vault.test.database.access.single.sales

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.test.database.factory.sales.PurchaseFactory
import utopia.vault.test.database.model.sales.PurchaseModel
import utopia.vault.test.model.stored.sales.Purchase

/**
  * Used for accessing individual purchases
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object DbPurchase extends SingleRowModelAccess[Purchase] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = PurchaseModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = PurchaseFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted purchase
	  * @return An access point to that purchase
	  */
	def apply(id: Int) = DbSinglePurchase(id)
}

