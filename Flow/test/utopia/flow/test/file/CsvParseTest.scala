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
	
	/*
	// Separator-splitting
	{
		println(CsvRows.defaultSeparator)
		
		val row = "v6, value containing \"\"escaped quotes\"\", ' v6"
			.splitIterator(CsvRows.defaultSeparator).map { _.trim }.toVector
		println(row.iterator.map { part => s"`$part`" }.mkString(", "))
		assert(row.size == 3)
		assert(row.head == "v6")
		assert(row(1) == "value containing \"\"escaped quotes\"\"")
		assert(row(2) == "' v6")
		
		val row2 = "v4,\"this column contains".splitIterator(CsvRows.defaultSeparator).map { _.trim }.toVector
		println(row2.iterator.map { part => s"`$part`" }.mkString(", "))
		assert(row2.size == 2)
		assert(row2.head == "v4")
		assert(row2(1) == "\"this column contains")
	}*/
	
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
	
	// multi-line
	/*
	h1,h2,h3
	v1,v2,v3
	v4,"this column contains
	multiple lines of text.
	A total of four
	lines of text, to be exact.",v5,v6
	v7,v8,"" is a broken quote
	this should be separate,v9,v10
	 */
	{
		val rows = CsvRows.from.path("Flow/data/test-material/multiline-test.csv").get
		println()
		println(rows.mkString("\n"))
		
		assert(rows.size == 4)
		assert(rows.head == Model.from("h1" -> "v1", "h2" -> "v2", "h3" -> "v3"))
		assert(rows(1) == Model.from("h1" -> "v4",
			"h2" -> "this column contains\nmultiple lines of text.\nA total of four\nlines of text, to be exact.",
			"h3" -> "v5"))
		assert(rows(2) == Model.from("h1" -> "v7", "h2" -> "v8", "h3" -> "\" is a broken quote"))
		assert(rows(3) == Model.from("h1" -> "this should be separate", "h2" -> "v9", "h3" -> "v10"))
	}
	
	println("Success!")
}
