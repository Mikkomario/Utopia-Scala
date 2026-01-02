package utopia.flow.test

import utopia.flow.operator.Identity
import utopia.flow.parse.string.Regex
import utopia.flow.parse.string.Regex.stringToRegex
import utopia.flow.util.StringExtensions._
import utopia.flow.util.StringUtils

/**
 * Tests new string methods
 * @author Mikko Hilpinen
 * @since 1.11.2019, v1.6.1+
 */
object StringUtilsTest extends App
{
	val s = "This is a test string"
	
	assert("a".contains(""))
	assert("a".indexOf("") == 0)
	assert("".indexOf("") == 0)
	
	assert(s.words == Vector("This", "is", "a", "test", "string"))
	assert(s.firstWord == "This")
	assert(s.lastWord == "string")
	// assert(s.notEmpty.isDefined)
	// assert("".notEmpty.isEmpty)
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
	assert(s.findIndexOf("s").contains(3))
	assert(s.findIndexOf("XAA").isEmpty)
	assert(s.findIndexOf("IS", ignoreCase = true).contains(2), s.findIndexOf("IS", ignoreCase = true))
	assert(s.dropUntil("test") == "test string")
	assert(s.dropUntilLast("s") == "string")
	assert(s.untilFirst("is") == "Th")
	assert(s.untilLast("is") == "This ")
	assert("banana".startingWith("abba", enablePartialReplacement = true) == "abbanana")
	assert("abba".startingWith("ab") == "abba")
	assert("abba".startingWith("abab") == "abababba")
	assert("nana".endingWith("apple", enablePartialReplacement = true) == "nanapple")
	assert("nana".endingWith("na") == "nana")
	assert("nana".endingWith("nas") == "nananas")
	assert(s.containsInOrder("is", "test"))
	assert(s.containsInOrder("This", "a", "string"))
	assert(!s.containsInOrder("this", "string"))
	
	assert("a,b,\"c,d,e\",f,,h".split(",".ignoringQuotations).toVector ==
		Vector("a", "b", "\"c,d,e\"", "f", "", "h"))
	
	assert("Almost.There.No.More.".divideWith(".") == Vector("Almost.", "There.", "No.", "More."))
	assert("Test".divideWith(".") == Vector("Test"))
	assert("Foo---bar".divideWith("---") == Vector("Foo---", "bar"))
	
	assert(Regex.digit.separate("A1BAA3D") == Vector("A1", "BAA3", "D"))
	assert(Regex.digit.extract("A1BAA3D") == (Vector("A", "BAA", "D"), Vector("1", "3")))
	assert(Regex.parentheses.extract("Some(more)text") == (Vector("Some", "text"), Vector("(more)")))
	assert(Regex("a").ignoringWithin('(', ')').findAllFrom("a test (another test)").size == 1)
	
	assert((Regex("import ") + Regex.any)("import java.time.Instant"))
	
	val control = "\"This is a test \nstring\""
	
	println(control)
	println(control.stripControlCharacters)
	
	assert(control.stripControlCharacters == "\"This is a test string\"")
	
	assert("XtestXX2".splitIterator("X").toVector == Vector("test", "2"))
	assert("XtestXX2".splitIterator("A").toVector == Vector("XtestXX2"))
	
	println("YV- 2716".filterWith(Regex.letterOrDigit))
	
	println("---")
	println(StringUtils.asciiTableFrom[String](Vector("ABC", "Test", "2-line\nString"),
		Vector(
			"Str" -> Identity, "Length" -> { _.length.toString }, "Letter" -> { _.head.toString }
		),
		"Test table 1"))
	println(StringUtils.asciiTableFrom[String](Vector("ABC", "Test", "2-line\nString"),
		Vector(
			"Str" -> Identity, "Length" -> { _.length.toString }, "Letter" -> { _.head.toString }
		),
		"Test table 2\nSome other text", skipLineSeparators = true))
	println("---")
	
	println()
	
	println("This is a long string that needs to be split to multiple lines. \n\nAlso, this string contains line breaks.\nSome."
		.splitToLinesIterator(20).mkString("\n"))
	
	// Tests notContainingAnyOf
	
	assert(s.notContainingAnyOf(Vector("str", "i", "a")) == "Ths s  test ng")
	assert("El Padel Oy".notContainingAnyOf(Set("Oy", "GmbH", "LLC", "Ry")) == "El Padel ")
	
	assert(s.overlapWith("Another test") == " test", s.overlapWith("Another test"))
	
	// Tests slice
	assert("TESTI".slice(0 until 3) == "TES", "TESTI".slice(0 until 3))
	assert("TESTI".slice(0 until 5) == "TESTI", "TESTI".slice(0 until 5))
	
	// Tests afterFirst, untilLast, etc.
	assert(s.afterFirst("test") == " string")
	assert(s.afterLast("st") == "ring")
	assert(s.untilFirst("is") == "Th")
	assert(s.untilLast("t") == "This is a test s")
	
	assert("asd".quoted == "\"asd\"")
	
	println("Success!")
}
