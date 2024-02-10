package utopia.genesis.test.unit

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.graphics.{DrawSettings, StrokeSettings}
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.polygon.Triangle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.nio.file.Path

/**
  * Tests drawing different shapes on an image
  * @author Mikko Hilpinen
  * @since 29.7.2023, v3.4
  */
object ShapesToImageTest extends App
{
	ParadigmDataType.setup()
	
	val lineDs = StrokeSettings(Color.gray(0.7))
	val shapeDs = DrawSettings.onlyFill(Color.blue.withAlpha(0.75))
	
	val imageSize = Size.square(80.0)
	val image = Image.paint(imageSize) { drawer =>
		// Draws background
		drawer.draw(Bounds(Point.origin, imageSize))(DrawSettings.onlyFill(Color.white))
		// Draws a grid for measuring
		(0 until 80 by 20).foreach { t =>
			drawer.draw(Line(Point(t, 0), Point(t, 80)))(lineDs)
			drawer.draw(Line(Point(0, t), Point(100, t)))(lineDs)
		}
		// Draws a triangle
		drawer.draw(Triangle(Point(60, 60), Vector2D(-20, 0), Vector2D(0, -20)))(shapeDs)
	}
	
	// Saves and opens the image
	val imageFile: Path = "Genesis/test-images/shapes-test.png"
	image.writeToFile(imageFile).get
	imageFile.openInDesktop().get
	println("Done!")
}
