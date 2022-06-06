package utopia.vault.test.database.factory.sales

import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.test.database.VaultTestTables
import utopia.vault.test.model.partial.sales.SalesProductData
import utopia.vault.test.model.stored.sales.SalesProduct

/**
  * Used for reading sales product data from the DB
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object SalesProductFactory 
	extends FromValidatedRowModelFactory[SalesProduct] with FromRowFactoryWithTimestamps[SalesProduct]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = VaultTestTables.salesProduct
	
	override def fromValidatedModel(valid: Model) = 
		SalesProduct(valid("id").getInt, SalesProductData(valid("name").getString, 
			valid("unitPrice").getDouble, valid("created").getInstant, valid("vipOnly").getBoolean))
}

