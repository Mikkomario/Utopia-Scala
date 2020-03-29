package utopia.flow.test

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
	
	println("Success!")
}
