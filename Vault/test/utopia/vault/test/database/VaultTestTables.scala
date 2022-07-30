package utopia.vault.test.database

import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
object VaultTestTables
{
	// COMPUTED	--------------------
	
	/**
	  * Table that contains purchases
	  */
	def purchase = apply("purchase")
	
	/**
	  * Table that contains sales products
	  */
	def salesProduct = apply("sales_product")
	
	/**
	  * Table that contains electronic signatures (Represents a received e-signature)
	  */
	def electronicSignature = apply("electronic_signature")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = TestTables(tableName)
}

