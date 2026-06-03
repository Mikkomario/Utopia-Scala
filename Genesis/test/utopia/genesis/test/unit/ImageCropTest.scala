package utopia.genesis.test.unit

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.transform.Adjustment

import java.nio.file.Path

/**
  * Tests the .cropped function in Image
  * @author Mikko Hilpinen
  * @since 16/01/2024, v3.5
  */
object ImageCropTest extends App
{
	ParadigmDataType.setup()
	
	private implicit val adj: Adjustment = Adjustment(0.4)
	
	private val directory: Path = "Genesis/test-images"
	private val originalPath = directory/"flight-find.png"
	private val editPath = originalPath.mapFileNameWithoutExtension { fName => s"$fName-cropped" }
	private val editPath2 = originalPath.mapFileNameWithoutExtension { fName => s"$fName-large-cropped" }
	
	private val original = Image.readFrom(originalPath).get
	private val cropped = original.cropped
	private val largeCropped = original.smaller.cropped
	
	cropped.writeToFile(editPath).get
	largeCropped.writeToFile(editPath2).get
	
	originalPath.openInDesktop()
	editPath.openInDesktop()
	editPath2.openInDesktop()
	
	println("Done!")
}
