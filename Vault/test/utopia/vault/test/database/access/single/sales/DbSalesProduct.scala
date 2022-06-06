package utopia.vault.test.database.access.single.sales

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.test.database.factory.sales.SalesProductFactory
import utopia.vault.test.database.model.sales.SalesProductModel
import utopia.vault.test.model.stored.sales.SalesProduct

/**
  * Used for accessing individual sales products
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object DbSalesProduct extends SingleRowModelAccess[SalesProduct] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SalesProductModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SalesProductFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted sales product
	  * @return An access point to that sales product
	  */
	def apply(id: Int) = DbSingleSalesProduct(id)
}

