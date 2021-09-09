package utopia.vault.coder.test

/**
  * Used for testing name conversion
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
object NameConversionTest extends App
{
	import utopia.vault.coder.util.NamingUtils.camelToUnderscore
	def test(source: String, expected: String) =
	{
		val result = camelToUnderscore(source)
		if (result != expected)
		{
			println(s"$source => $result (expected $expected)")
			assert(false)
		}
	}
	
	test("test", "test")
	test("anotherTest", "another_test")
	test("theThirdTest", "the_third_test")
	test("convertRGB", "convert_rgb")
	
	println("Success!")
}
