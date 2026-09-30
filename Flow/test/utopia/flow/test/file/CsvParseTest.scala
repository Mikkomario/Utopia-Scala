package utopia.flow.test.file

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.file.{CsvRows, FileUtils}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._

/**
 * Tests CSV-parsing
 * @author Mikko Hilpinen
 * @since 29.09.2026, v2.9
 */
object CsvParseTest extends App
{
	private implicit val jsonParser: JsonParser = JsonReader
	
	private val quoteR = Regex.escape('"')
	private val doubleQuoteR = quoteR * 2
	
	assert("value containing \"\"escaped quotes\"\"".replaceEachMatchOf(doubleQuoteR, "\"") == "value containing \"escaped quotes\"")
	
	// Separator-splitting
	{
		val row = "v6, value containing \"\"escaped quotes\"\", ' v6"
			.splitIterator(CsvRows.defaultSeparator).map { _.trim }.toVector
		println(row.iterator.map { part => s"`$part`" }.mkString(", "))
		assert(row.size == 3)
		assert(row.head == "v6")
		assert(row(1) == "value containing \"\"escaped quotes\"\"")
		assert(row(2) == "' v6")
	}
	
	// default (= single-line)
	/*
	h1,header 2, another header
	1, 2, value 3
	v4, "value, containing a comma",
	v6, value containing ""escaped quotes"", ' v6
	 */
	{
		println(FileUtils.workingDirectory.toAbsolutePath)
		val rows = CsvRows.from.path("Flow/data/test-material/test.csv").get
		println(rows.mkString("\n"))
		
		assert(rows.size == 3)
		assert(rows.head == Model.from("h1" -> 1, "header 2" -> 2, "another header" -> "value 3"))
		assert(rows(1) == Model.from("h1" -> "v4", "header 2" -> "value, containing a comma",
			"another header" -> Value.empty))
		assert(rows(2) == Model.from("h1" -> "v6", "header 2" -> "value containing \"escaped quotes\"",
			"another header" -> " v6"))
	}
	
	println("Success!")
}
