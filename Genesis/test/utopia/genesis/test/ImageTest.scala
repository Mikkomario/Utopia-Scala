package utopia.genesis.test

import java.nio.file.Paths

import utopia.flow.async.ThreadPool
import utopia.genesis.handling.Drawable
import utopia.genesis.image.Image
import utopia.genesis.shape.{Angle, Rotation}
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.genesis.util.{DefaultSetup, Drawer}
import utopia.inception.handling.immutable.Handleable

import scala.concurrent.ExecutionContext

/**
  * This test tests image drawing
  * @author Mikko Hilpinen
  * @since 28.6.2019, v2.1+
  */
object ImageTest extends App
{
	// Generates the images
	val original = Image.readFrom(Paths.get("test-images/mushrooms.png")).get
		.withMaxSourceResolution(Size(128, 128)).withSize(Size(96, 96))
	val leftPartBounds = Bounds(Point.origin, Size(57, 96))
	val leftHalf = original.subImage(leftPartBounds)
	val partiallyMapped = original.mapArea(leftPartBounds) { _ + Rotation.ofDegrees(90) }
	
	// Sets up the program
	val gameWorldSize = Size(800, 300)
	
	val setup = new DefaultSetup(gameWorldSize, "Image Test")
	
	setup.registerObjects(new ImageDrawer(original, Point(50, 50)),
		new ImageDrawer(original.flippedHorizontally, Point(150, 50)),
		new ImageDrawer(original.flippedVertically, Point(250, 50)),
		new ImageDrawer(original.withIncreasedContrast, Point(350, 50)),
		new ImageDrawer(original.inverted, Point(450, 50)),
		new ImageDrawer(leftHalf, Point(550, 50)),
		new ImageDrawer(partiallyMapped, Point(650, 50)),
		new ImageDrawer(original.blurred(), Point(50, 150)),
		new ImageDrawer(original.sharpened(), Point(150, 150)),
		new ImageDrawer(original.withAdjustedHue(Angle.red, Angle.ofDegrees(90), Angle.blue), Point(250, 150)),
		new ImageDrawer(original.withThreshold(3), Point(350, 150)),
		new ImageDrawer(original * 2, Point(450, 150))
	)
	
	// Starts the program
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	setup.start()
}

private class ImageDrawer(val image: Image, position: Point) extends Drawable with Handleable
{
	val origin = image.size.toPoint / 2
	override def draw(drawer: Drawer) = drawer.drawImage(image, position, origin)
}