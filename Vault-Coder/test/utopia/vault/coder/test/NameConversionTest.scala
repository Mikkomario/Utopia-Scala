package utopia.vault.coder.test

import utopia.vault.coder.model.data.Name
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, Text, UnderScore}

/**
  * Used for testing name conversion
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
object NameConversionTest extends App
{
	val n1 = Name("test")
	val n2 = Name("test2", UnderScore)
	val n3 = Name.interpret("test3", UnderScore)
	val n4 = Name("anotherTest")
	val n5 = Name("more_test")
	val n6 = Name("Test Header")
	val n7 = Name("convertRGB")
	val n8 = Name("TestClass")
	
	assert(n1.style == CamelCase.lower)
	assert(n2.style == UnderScore)
	assert(n3.style == UnderScore)
	assert(n4.style == CamelCase.lower)
	assert(n5.style == UnderScore)
	assert(n6.style == Text.allCapitalized)
	assert(n7.style == CamelCase.lower)
	assert(n8.style == CamelCase.capitalized)
	
	def test(source: Name, camelLow: String, camelHigh: String, underScore: String, textLow: String, textHigh: String) =
	{
		val cL = source.to(CamelCase.lower).singular
		val cH = source.to(CamelCase.capitalized).singular
		val u = source.to(UnderScore).singular
		val tL = source.to(Text.lower).singular
		val tH = source.to(Text.allCapitalized).singular
		
		println(s"${source.singular} => $cL, $cH, $u, $tL, $tH")
		assert(cL == camelLow)
		assert(cH == camelHigh)
		assert(u == underScore)
		assert(tL == textLow)
		assert(tH == textHigh)
	}
	
	test(n1, "test", "Test", "test", "test", "Test")
	test(n2, "test2", "Test2", "test2", "test2", "Test2")
	test(n4, "anotherTest", "AnotherTest", "another_test", "another test",
		"Another Test")
	test(n5, "moreTest", "MoreTest", "more_test", "more test",
		"More Test")
	test(n6, "testHeader", "TestHeader", "test_header", "test header",
		"Test Header")
	test(n7, "convertRGB", "ConvertRGB", "convert_rgb", "convert rgb",
		"Convert RGB")
	test(n8, "testClass", "TestClass", "test_class", "test class",
		"Test Class")
	
	println("Success!")
}
