package utopia.vault.test.app

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.test.TestContext._
import utopia.vault.database.columnlength.{ColumnLengthLimits, ColumnLengthRules}
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.sql.{Delete, Insert, Select, SelectAll}
import utopia.vault.test.database.TestTables

/**
  * Tests length-rule application in practice
  * @author Mikko Hilpinen
  * @since 18.10.2022, v1.14.1
  */
object LengthRulesTest2 extends App
{
	implicit val jsonParser: JsonParser = JsonReader
	implicit val cPool: ConnectionPool = new ConnectionPool()
	
	val dbName = "vault_test"
	Connection.modifySettings { _.copy(defaultDBName = Some(dbName), charsetName = "utf8",
		charsetCollationName = "utf8_general_ci") }
	
	ColumnLengthRules.loadFrom("Vault/test-data/length-rules/lengthlimits.json")
	
	println()
	println(ColumnLengthRules)
	println()
	println(ColumnLengthLimits)
	println()
	
	val table = TestTables.lengthTest
	cPool { implicit con =>
		con(Delete(table))
		val id1 = Insert(table, Model.from("str" -> "12345")).generatedKeys.head.getInt
		val id2 = Insert(table, Model.from("str" -> "123456789")).generatedKeys.head.getInt
		val id3 = Insert(table, Model.from("num" -> 65)).generatedKeys.head.getInt
		val id4 = Insert(table, Model.from("num" -> 124)).generatedKeys.head.getInt
		
		val items = con(Select.all(table)).rows.map { _.toModel }.map { m => m("id").getInt -> m }.toMap
		
		assert(items(id1)("str").getString == "12345")
		assert(items(id3)("num").getInt == 65)
		
		println(items(id2)("str"))
		println(items(id4)("num"))
		
		println("Success!")
	}
}
