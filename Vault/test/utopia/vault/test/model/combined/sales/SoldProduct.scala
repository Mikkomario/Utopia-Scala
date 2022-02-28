package utopia.vault.test.model.combined.sales

import utopia.flow.util.Extender
import utopia.vault.test.model.partial.sales.SalesProductData
import utopia.vault.test.model.stored.sales.{Purchase, SalesProduct}

/**
  * Combines product with purchase data
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
case class SoldProduct(product: SalesProduct, purchases: Vector[Purchase]) extends Extender[SalesProductData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this product in the database
	  */
	def id = product.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = product.data
}

