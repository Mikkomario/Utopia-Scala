package utopia.vault.test.database.factory.sales

import utopia.vault.nosql.factory.multi.MultiCombiningFactory
import utopia.vault.test.model.combined.sales.SoldProduct
import utopia.vault.test.model.stored.sales.{Purchase, SalesProduct}

/**
  * Used for reading sold products from the database
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object SoldProductFactory extends MultiCombiningFactory[SoldProduct, SalesProduct, Purchase]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = PurchaseFactory
	
	override def isAlwaysLinked = false
	
	override def parentFactory = SalesProductFactory
	
	override def apply(product: SalesProduct, purchases: Vector[Purchase]) = SoldProduct(product, purchases)
}

