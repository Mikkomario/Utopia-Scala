package utopia.genesis.test.unit

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.image.Image

/**
  * Encodes an image into Base 64 and then decodes it
  * @author Mikko Hilpinen
  * @since 20.07.2024, v4.0
  */
object ImageEncodeDecodeTest extends App
{
	private val original = Image.readFrom("Genesis/test-images/close.png").get
	private val encoded = original.toBase64String.get
	println(encoded)
	
	private val decoded = Image.fromBase64EncodedString(encoded).get
	decoded.writeToFile("Genesis/test-images/close-decoded.png")
	
	println("Success!")
}
