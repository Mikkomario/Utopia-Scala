package utopia.flow.test

import utopia.flow.util.Version

/**
 * Tests version number parsing / handling
 * @author Mikko Hilpinen
 * @since 3.10.2021, v1.12.1
 */
object VersionTest extends App
{
	val v1 = Version("v1.0")
	
	println(v1.toString)
	assert(v1.toString == "v1.0")
	assert(v1.major == 1)
	assert(v1.minor == 0)
	assert(v1.patch == 0)
	assert(v1 == Version("1.0"))
	assert(v1 == Version("1"))
	
	val v2 = Version("v2.3.1-alpha")
	
	assert(v2.major == 2)
	assert(v2.minor == 3)
	assert(v2.patch == 1)
	assert(v2.suffix == "alpha")
	
	def testMatch(from: String, expected: String) =
		assert(Version.findFrom(from).exists { _.toString == expected })
	
	testMatch("Some text v2.1", "v2.1")
	testMatch("Something something 2.2 more", "v2.2")
	testMatch("1 Apple", "v1.0")
	testMatch("1-Apple", "v1.0-Apple")
	testMatch("Text v3.2.1-alpha-update and more text", "v3.2.1-alpha-update")
	testMatch("1 Apple and 2 Oranges v2.0-beta, right?", "v2.0-beta")
	
	def testNotFound(from: String) = assert(Version.findFrom(from).isEmpty)
	
	testNotFound("Some text here")
	testNotFound("A.B.C")
	testNotFound("-alpha")
	
	println("Success!")
}
