package utopia.genesis.test

import utopia.genesis.image.Image
import utopia.paradigm.enumeration.Axis.Y

import java.nio.file.Paths

/**
  * Used for testing image size functions
  * @author Mikko Hilpinen
  * @since 28.11.2022, v3.1.1
  */
object ImageSizeTest extends App
{
	val img = Image.readFrom(Paths.get("Genesis/test-images/mushrooms.png")).get * 2.0
	
	println(img.size)
	
	assert(img.fittingWithin(Y(20)).width > 0)
	
	println("Success!")
}
