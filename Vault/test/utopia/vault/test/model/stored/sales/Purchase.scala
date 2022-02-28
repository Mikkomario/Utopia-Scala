package utopia.vault.test.model.stored.sales

import utopia.vault.model.template.StoredModelConvertible
import utopia.vault.test.database.access.single.sales.DbSinglePurchase
import utopia.vault.test.model.partial.sales.PurchaseData

/**
  * Represents a purchase that has already been stored in the database
  * @param id id of this purchase in the database
  * @param data Wrapped purchase data
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
case class Purchase(id: Int, data: PurchaseData) extends StoredModelConvertible[PurchaseData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this purchase in the database
	  */
	def access = DbSinglePurchase(id)
}

