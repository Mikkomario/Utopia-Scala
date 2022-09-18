package utopia.genesis.test

import utopia.flow.test.TestContext._
import utopia.genesis.handling.Drawable
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.{Bounds, Matrix2D, Point, Size}
import utopia.genesis.util.{DefaultSetup, Drawer}
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.generic.ParadigmDataType

import java.nio.file.Paths

/**
  * This test tests image drawing
  * @author Mikko Hilpinen
  * @since 28.6.2019, v2.1+
  */
object ImageTest extends App
{
	ParadigmDataType.setup()
	
	// Generates the images
	val original = Image.readFrom(Paths.get("Genesis/test-images/mushrooms.png")).get
		.withMaxSourceResolution(Size(128, 128)).withSize(Size(100, 100)).withCenterOrigin
	val leftPartBounds = Bounds(Point.origin, Size(57, 96))
	val leftHalf = original.subImage(leftPartBounds).withCenterOrigin
	val partiallyMapped = original.mapArea(leftPartBounds) { _ + Rotation.ofDegrees(90) }
	val combined = original.withOverlay(original * 0.5)
	
	// Sets up the program
	val gameWorldSize = Size(900, 300)
	
	val setup = new DefaultSetup(gameWorldSize, "Image Test")
	
	setup.registerObjects(
		new GridDrawer(gameWorldSize, Size(50, 50)),
		new ImageDrawer(original, Point(50, 50)),
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
		new ImageDrawer(original.withoutSpecifiedOrigin * 1.5, Point(450, 150)),
		new ImageDrawer(combined, Point(550, 150)),
		new ImageDrawer(leftHalf.transformedWith(Matrix2D.quarterRotationCounterClockwise), Point(650, 150))
	)
	
	// Starts the program
	setup.start()
}

private class ImageDrawer(val image: Image, position: Point) extends Drawable with Handleable
{
	override def draw(drawer: Drawer) =
	{
		image.drawWith(drawer, position/*, Some(Matrix2D.quarterRotationCounterClockwise)*/)
		// drawer.onlyFill(Color.red).draw(Circle(position, 3))
	}
}