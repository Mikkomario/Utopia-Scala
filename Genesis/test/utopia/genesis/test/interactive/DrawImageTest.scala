package utopia.genesis.test.interactive

import utopia.flow.test.TestContext._
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler}
import utopia.genesis.handling.{ActorLoop, Drawable}
import utopia.genesis.image.Image
import utopia.genesis.view.{Canvas, CanvasMouseEventGenerator, MainFrame}
import utopia.inception.handling.HandlerType
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * Tests drawing on an image.
  * When running the test, you should be able to see four greenish shapes, one at each corner of the screen.
  * There should also be a yellow square near the top.
  * @author Mikko Hilpinen
  * @since 24.1.2023, v3.2
  */
object DrawImageTest extends App
{
	// TEST ----------------------------
	
	implicit val ds: DrawSettings = DrawSettings.onlyFill(Color.green)
	val ds2 = DrawSettings(Color.yellow)(StrokeSettings(Color.red, 3))
	
	// Creates the handlers
	val gameWorldSize = Size(800, 800)
	
	val drawHandler = DrawableHandler()
	val actorHandler = ActorHandler()
	
	val canvas = new Canvas(drawHandler, gameWorldSize)
	val mouseEventGen = new CanvasMouseEventGenerator(canvas)
	
	val handlers = HandlerRelay(drawHandler, actorHandler, mouseEventGen.buttonHandler, mouseEventGen.moveHandler,
		mouseEventGen.wheelHandler)
	
	// Creates event generators
	val actorLoop = new ActorLoop(actorHandler, 10 to 120)
	
	// Creates test objects
	val t1 = new ImageDrawer(Point.origin, { d => d.draw(Circle(Point(200, 200), 200)) },
		Some({ d =>
			// d.scaled(2).draw(Bounds.between(Point(10, 10), Point(30, 30)))(ds2)
			d.draw(Bounds.between(Point(10, 10), Point(30, 30)))(ds2)
		}))
	val t2 = new ImageDrawer(Point(0, 400), { d =>
		d.draw(Bounds.between(Point(10, 10), Point(390, 390)))
		d.draw(Circle(Point(200, 200), 100))(ds2)
	})
	
	handlers ++= Vector(t1, t2)
	
	// Creates the frame
	val frame = new MainFrame(canvas, gameWorldSize, "Image Drawing Test")
	
	actorHandler += mouseEventGen
	
	// Displays the frame
	actorLoop.runAsync()
	canvas.startAutoRefresh()
	
	frame.display()
	
	
	// NESTED   -------------------------
	
	class ImageDrawer(topLeft: Point, f: Drawer => Unit, overlay: Option[Drawer => Unit] = None) extends Drawable
	{
		val image = {
			val base = Image.paint(Size(400, 400))(f)
			overlay match {
				case Some(f) => base.paintedOver(f)
				case None => base
			}
		}
		
		override def draw(drawer: Drawer): Unit = {
			drawer.translated(topLeft).use { drawer =>
				image.drawWith(drawer, Point(400, 0))
				f(drawer)
			}
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType): Boolean = true
	}
}
