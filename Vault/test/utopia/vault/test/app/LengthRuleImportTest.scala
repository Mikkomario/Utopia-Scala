package utopia.vault.test.app

import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.json.{JsonReader, JsonParser}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.test.TestContext._
import utopia.vault.database.ConnectionPool
import utopia.vault.database.columnlength.ColumnLengthRules

/**
  * Tests length rule loading
  * @author Mikko Hilpinen
  * @since 8.6.2022, v1.12.2
  */
object LengthRuleImportTest extends App
{
	implicit val jsonParser: JsonParser = JsonReader
	implicit val cPool: ConnectionPool = new ConnectionPool()
	
	DataType.setup()
	ColumnLengthRules
		.loadFrom("Citadel/data/length-rules/citadel-length-rules-v2.1.json", "test").get
	println(ColumnLengthRules.apply("test", "description"))
}
