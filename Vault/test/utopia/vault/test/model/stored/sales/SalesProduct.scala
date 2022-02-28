package utopia.vault.test.model.stored.sales

import utopia.vault.model.template.StoredModelConvertible
import utopia.vault.test.database.access.single.sales.DbSingleSalesProduct
import utopia.vault.test.model.partial.sales.SalesProductData

/**
  * Represents a sales product that has already been stored in the database
  * @param id id of this sales product in the database
  * @param data Wrapped sales product data
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
case class SalesProduct(id: Int, data: SalesProductData) extends StoredModelConvertible[SalesProductData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this sales product in the database
	  */
	def access = DbSingleSalesProduct(id)
}

