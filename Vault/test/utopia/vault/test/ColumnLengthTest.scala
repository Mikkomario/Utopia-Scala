package utopia.vault.test

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.DataType
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.util.FileExtensions._
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.database.columnlength.ColumnLengthRules
import utopia.vault.database.columnlength.ColumnNumberLimit.IntLimit
import utopia.vault.sql.{Delete, Insert}

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Tests column length rule application
  * @author Mikko Hilpinen
  * @since 27.1.2022, v1.12
  */
object ColumnLengthTest extends App
{
	DataType.setup()
	implicit val exc: ExecutionContext = TestThreadPool.executionContext
	implicit val connPool: ConnectionPool = TestConnectionPool
	implicit val jsonReader: JsonParser = JSONReader
	ColumnLengthRules.loadFrom("Vault/test/testData/lengthlimits.json").get
	
	Connection.doTransaction { implicit connection =>
		Delete(Person.table).execute()
		
		def insert(name: String, age: Int) = Person(name, Some(age)).insert().getInt
		val normalId = insert("test", 5)
		val expandNameId = insert("1234567890123456789012345678901234567890M", 5)
		val hugeAgeId = Insert(Person.table, Model(Vector("name" -> "test 3", "age" -> (IntLimit.maxValue + 10))))
			.generatedKeys.head.getInt
		
		val persons = Person.getAll().map { p => p.rowId.get -> p }.toMap
		println(s"${persons.size} entries:")
		persons.foreach { p => println(s"${p._2.name}, ${p._2.age.getOrElse(0)} years") }
		
		assert(persons(normalId).name == "test")
		assert(persons(normalId).age.contains(5))
		assert(persons(expandNameId).name == "1234567890123456789012345678901234567890M")
		assert(persons(hugeAgeId).age.contains(IntLimit.maxValue.toInt))
		
		Try {
			insert("X" * 128, 1)
		}.foreach { _ => throw new IllegalStateException("Too long insert succeeded") }
	}
	
	println("Done!")
}
