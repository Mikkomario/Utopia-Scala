package utopia.vault.test.app

import utopia.flow.async.ThreadPool
import utopia.flow.generic.DataType
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.util.FileExtensions._
import utopia.vault.database.ConnectionPool
import utopia.vault.database.columnlength.ColumnLengthRules

import scala.concurrent.ExecutionContext

/**
  * Tests length rule loading
  * @author Mikko Hilpinen
  * @since 8.6.2022, v1.12.2
  */
object LengthRuleImportTest extends App
{
	implicit val jsonParser: JsonParser = JSONReader
	implicit val exc: ExecutionContext = new ThreadPool("test").executionContext
	implicit val cPool: ConnectionPool = new ConnectionPool()
	
	DataType.setup()
	ColumnLengthRules
		.loadFrom("Citadel/data/length-rules/citadel-length-rules-v2.1.json", "test").get
	println(ColumnLengthRules.apply("test", "description"))
}