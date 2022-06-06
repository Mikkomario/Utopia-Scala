package utopia.vault.test.database.factory.sales

import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.test.database.VaultTestTables
import utopia.vault.test.model.partial.sales.PurchaseData
import utopia.vault.test.model.stored.sales.Purchase

/**
  * Used for reading purchase data from the DB
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object PurchaseFactory 
	extends FromValidatedRowModelFactory[Purchase] with FromRowFactoryWithTimestamps[Purchase]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = VaultTestTables.purchase
	
	override def fromValidatedModel(valid: Model) = 
		Purchase(valid("id").getInt, PurchaseData(valid("productId").getInt, valid("unitsBought").getInt, 
			valid("estimatedDelivery").localDate, valid("created").getInstant))
}

