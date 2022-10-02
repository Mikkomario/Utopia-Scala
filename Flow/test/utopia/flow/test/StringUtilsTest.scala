package utopia.flow.test

import utopia.flow.parse.string.Regex.stringToRegex
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._

/**
 * Tests new string methods
 * @author Mikko Hilpinen
 * @since 1.11.2019, v1.6.1+
 */
object StringUtilsTest extends App
{
	val s = "This is a test string"
	
	assert(s.words == Vector("This", "is", "a", "test", "string"))
	assert(s.firstWord == "This")
	assert(s.lastWord == "string")
	assert(s.notEmpty.isDefined)
	assert("".notEmpty.isEmpty)
	assert("12".toIntOption.contains(12))
	assert("a".toIntOption.isEmpty)
	assert("-12.3".toDoubleOption.contains(-12.3))
	assert("a".toDoubleOption.isEmpty)
	assert(s.letters == "Thisisateststring")
	assert("Test string 12".digits == "12")
	assert(s.containsIgnoreCase("this"))
	assert(!s.containsIgnoreCase("Asd"))
	assert(s.startsWithIgnoreCase("this"))
	assert(!s.startsWithIgnoreCase("Asd"))
	assert(s.containsAll("This", "a", "string"))
	assert(!s.containsAll("this", "string"))
	assert(s.containsAllIgnoreCase("this", "string"))
	assert(s.endsWithIgnoreCase("STRING"))
	assert(s.optionIndexOf("s").contains(3))
	assert(s.optionIndexOf("XAA").isEmpty)
	assert(s.dropUntil("test") == "test string")
	assert(s.dropUntilLast("s") == "string")
	assert(s.untilFirst("is") == "Th")
	assert(s.untilLast("is") == "This ")
	
	assert("a,b,\"c,d,e\",f,,h".split(",".ignoringQuotations).toVector ==
		Vector("a", "b", "\"c,d,e\"", "f", "", "h"))
	
	assert("Almost.There.No.More.".divideWith(".") == Vector("Almost.", "There.", "No.", "More."))
	assert("Test".divideWith(".") == Vector("Test"))
	assert("Foo---bar".divideWith("---") == Vector("Foo---", "bar"))
	
	assert(Regex.digit.separate("A1BAA3D") == Vector("A1", "BAA3", "D"))
	assert(Regex.digit.extract("A1BAA3D") == (Vector("A", "BAA", "D"), Vector("1", "3")))
	assert(Regex.parenthesis.extract("Some(more)text") == (Vector("Some", "text"), Vector("(more)")))
	assert(Regex("a").ignoringWithin('(', ')').findAllFrom("a test (another test)").size == 1)
	
	assert((Regex("import ") + Regex.any)("import java.time.Instant"))
	
	val control = "\"This is a test \nstring\""
	
	println(control)
	println(control.stripControlCharacters)
	
	assert(control.stripControlCharacters == "\"This is a test string\"")
	
	assert("XtestXX2".splitIterator("X").toVector == Vector("test", "2"))
	
	println("YV- 2716".filterWith(Regex.alphaNumeric))
	
	println("Success!")
}
