package utopia.genesis.test

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.generic.ParadigmDataType

import java.nio.file.Path

/**
  * Tests the .cropped function in Image
  * @author Mikko Hilpinen
  * @since 16/01/2024, v3.5
  */
object ImageCropTest extends App
{
	ParadigmDataType.setup()
	
	private val directory: Path = "Genesis/test-images"
	private val originalPath = directory/"speech-bubble.png"
	private val editPath = originalPath.withMappedFileName { fName =>
		s"${fName.untilLast(".")}-cropped.${fName.afterLast(".")}"
	}
	
	private val original = Image.readFrom(originalPath).get
	private val cropped = original.cropped
	
	cropped.writeToFile(editPath).get
	
	originalPath.openInDesktop()
	editPath.openInDesktop()
	
	println("Done!")
}
