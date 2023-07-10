package utopia.vault.test.app

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.test.database.access.many.sales.DbSalesProducts
import utopia.vault.test.database.access.single.sales.DbSalesProduct
import utopia.vault.test.database.model.sales.SalesProductModel
import utopia.vault.test.database.{TestConnectionPool, TestThreadPool}
import utopia.vault.test.model.partial.sales.SalesProductData

import scala.concurrent.ExecutionContext

/**
  * Tests database accessing
  * @author Mikko Hilpinen
  * @since 28.2.2022, v1.12.1
  */
object AccessTest extends App
{
	implicit val exc: ExecutionContext = TestThreadPool
	
	Connection.modifySettings { _.copy(defaultDBName = Some("vault_test"), charsetName = "utf8",
		charsetCollationName = "utf8_general_ci") }
	
	def many = DbSalesProducts
	def single = DbSalesProduct
	def model = SalesProductModel
	
	TestConnectionPool { implicit c =>
		// Deletes previous test data
		many.delete()
		assert(many.isEmpty)
		
		// Inserts test data
		val inserted = model.insert(Vector(
			SalesProductData("test-product-1", 1.0),
			SalesProductData("test-product-2", 2.0),
			SalesProductData("test-product-3", 3.0, vipOnly = true),
			SalesProductData("test-product-4", 4.0, vipOnly = true)
		))
		
		// Tests data pull & find
		assert(many.size == 4)
		assert(many.find(model.unitPriceColumn > 2.0).size == 2)
		assert(many.find(model.withVipOnly(true).toCondition).size == 2)
		assert(many.find(model.withVipOnly(true).toCondition && model.unitPriceColumn < 3.5).size == 1)
		assert(many.find(model.withVipOnly(true).toCondition || model.unitPriceColumn < 2.0).size == 3)
		assert(single.find(model.withUnitPrice(1.0).toCondition).exists { _.name == "test-product-1" })
		assert(single(inserted.head.id).name.contains("test-product-1"))
		
		println("Success!")
	}
}
