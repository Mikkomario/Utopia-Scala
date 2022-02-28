package utopia.vault.test.database.access.single.sales

import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.vault.test.model.stored.sales.SalesProduct

/**
  * An access point to individual sales products, based on their id
  * @author Mikko Hilpinen
  * @since 28.02.2022, v1.12.1
  */
case class DbSingleSalesProduct(id: Int) 
	extends UniqueSalesProductAccess with SingleIntIdModelAccess[SalesProduct]

