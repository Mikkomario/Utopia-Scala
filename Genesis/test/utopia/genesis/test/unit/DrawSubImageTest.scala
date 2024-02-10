package utopia.genesis.test.unit

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.graphics.{DrawSettings, StrokeSettings}
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.nio.file.Path

/**
  * Tests the sub-image drawing feature
  *
  * What to expect:
  *     - A directory should open, containing image files named generated-1.png and generated-2-png.
  *     - Generated 1 should show 5 red squares, one within another (32 x 32 pxl)
  *     - Generated 2 should show the bottom right quarter of the first image
  *      (including empty space in the 3/4 quarters) with size 64 x 64
  *
  * @author Mikko Hilpinen
  * @since 09/02/2024, v4.0
  */
object DrawSubImageTest extends App
{
	private implicit val ds: DrawSettings = StrokeSettings(Color.red)
	private val dir: Path = "Genesis/test-images"
	
	private val img1Size = Size.square(32)
	private val img1 = Image.paint(img1Size) { drawer =>
		val fullBounds = Bounds(Point.origin, img1Size)
		(2 until 12 by 2).foreach { r => drawer.draw(fullBounds.shrunk(r)) }
	}
	
	img1.writeToFile(dir/"generated-1.png").get
	
	private val img2 = Image.paint(img1Size * 2) { drawer =>
		val scaled = img1 * 2
		scaled.drawSubImageWith(drawer, Bounds(img1Size.toPoint, img1Size))
	}
	
	img2.writeToFile(dir/"generated-2.png").get
	
	dir.openInDesktop()
	println("Done!")
}
