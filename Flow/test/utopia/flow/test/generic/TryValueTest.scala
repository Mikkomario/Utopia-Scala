package utopia.flow.test.generic

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.casting.ValueConversions._

/**
  * Tests the try accessors in Value
  * @author Mikko Hilpinen
  * @since 16.07.2024, v2.4
  */
object TryValueTest extends App
{
	val empty = Value.empty
	
	assert(empty.tryString.get == "")
	assert(empty.tryInt.isFailure)
	assert(empty.tryVector.get == Vector.empty)
	assert("1".tryInt.get == 1)
	assert("a".tryInt.isFailure)
	assert("[1,2,3]".tryVectorWith { _.tryInt }.get == Vector(1, 2, 3))
	
	println("Success!")
}
