package utopia.vault.test.app

import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.json.{JsonReader, JsonParser}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.test.TestContext._
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.database.columnlength.{ColumnLengthLimits, ColumnLengthRules}
import utopia.vault.test.database.{TestTables, VaultTestTables}
import utopia.vault.test.database.model.operation.ElectronicSignatureModel
import utopia.vault.test.model.partial.operation.ElectronicSignatureData

import java.nio.file.Paths

/**
  * Tests length-rule application in practice
  * @author Mikko Hilpinen
  * @since 30.7.2022, v1.13
  */
object LengthRulesTest extends App
{
	DataType.setup()
	implicit val jsonParser: JsonParser = JsonReader
	implicit val cPool: ConnectionPool = new ConnectionPool()
	
	val dbName = "vault_test"
	Connection.modifySettings { _.copy(defaultDBName = Some(dbName), charsetName = "utf8",
		charsetCollationName = "utf8_general_ci") }
	TestTables.columnNameConversion = original => original.afterLast("_").notEmpty.getOrElse(original)
	
	Paths.get("Vault/test-data/length-rules")
		.tryIterateChildren { _.tryForeach { ColumnLengthRules.loadFrom(_, dbName) } }
		.get
	
	println()
	println(ColumnLengthRules)
	println()
	VaultTestTables.electronicSignature.columns.foreach { c =>
		println(s"${c.columnName} => ${c.name}")
	}
	println()
	println(ColumnLengthLimits)
	println()
	
	val sig = cPool { implicit c =>
		ElectronicSignatureModel.insert(ElectronicSignatureData(
			"/C=DE/ST=Hessen/L=Frankfurt/O=Lufthansa/CN=*.cloud.aero/emailAddress=test@example.com",
			"9223372036854775807"))
	}
	
	println(s"Successfully inserted $sig")
}
