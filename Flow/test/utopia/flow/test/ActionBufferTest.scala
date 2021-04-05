package utopia.flow.test

import utopia.flow.util.ActionBuffer

/**
 * Tests action buffer class
 * @author Mikko Hilpinen
 * @since 5.4.2021, v1.9
 */
object ActionBufferTest extends App
{
	var lastResult = ""
	val buffer = ActionBuffer[Char](3) { items => lastResult = items.mkString("") }
	
	buffer += 'A'
	
	assert(lastResult.isEmpty)
	
	buffer.flush()
	
	assert(lastResult == "A")
	
	buffer += 'B'
	buffer += 'C'
	
	assert(lastResult == "A")
	
	buffer += 'D'
	
	assert(lastResult == "BCD")
	
	buffer.flush()
	
	assert(lastResult == "BCD")
	
	buffer ++= "EFG"
	
	assert(lastResult == "EFG")
	
	buffer += 'A'
	buffer ++= "BCD"
	
	assert(lastResult == "ABC")
	
	buffer ++= "EFGHIJ"
	
	assert(lastResult == "GHI")
	
	buffer ++= "GG"
	
	assert(lastResult == "JGG")
	
	println("Success!")
}
