package utopia.genesis.test.unit

/**
  *
  * @author Mikko Hilpinen
  * @since 03/02/2024, v
  */
object KeyIndexTest extends App
{
	assert('['.toInt == 0x5B)
	assert(']'.toInt == 0x5D)
	assert('-'.toInt == 0x2D)
	assert('/'.toInt == 0x2F)
	assert('='.toInt == 0x3D)
	// 0x3D
	
	println(0x97.toChar)
	println(0x0209.toChar)
	assert('0' + 48 == 0x60)
}
