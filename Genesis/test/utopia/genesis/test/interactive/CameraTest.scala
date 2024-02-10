package utopia.genesis.test.interactive

import utopia.flow.test.TestContext._
import utopia.genesis.graphics.{Drawer, TextDrawHeight}
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler}
import utopia.genesis.handling.{ActorLoop, Drawable}
import utopia.genesis.util.Fps
import utopia.genesis.view.{Canvas, CanvasMouseEventGenerator, MainFrame}
import utopia.inception.handling.immutable.Handleable
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.awt.Font

object CameraTest extends App
{
	ParadigmDataType.setup()
	
    class GridNumberDrawer(private val grid: GridDrawer) extends Drawable with Handleable
    {
	    implicit val txh: TextDrawHeight = TextDrawHeight.Standard
	    
        private val font = new Font("Arial", 0, 14)
        
        override def draw(drawer: Drawer) = {
            // Draws a number on each grid square
            for (x <- 0 until grid.squareAmounts.x.toInt; y <- 0 until grid.squareAmounts.y.toInt) {
	            drawer.forTextDrawing(font).drawAround((y * grid.squareAmounts.x.toInt + x + 1).toString,
		            grid.squareCenter(x, y), Center)
            }
        }
    }
	
	// Creates handlers
	val actorHandler = ActorHandler()
	val drawHandler = DrawableHandler()
	
	// Creates frame
    val worldSize = Size(800, 600)
    
    val canvas = new Canvas(drawHandler, worldSize)
    val frame = new MainFrame(canvas, worldSize, "Camera Test")
    
	// Sets up generators
    val actorLoop = new ActorLoop(actorHandler, 20 to 120)
    val mouseEventGen = new CanvasMouseEventGenerator(canvas)
    actorHandler += mouseEventGen
    
	// Creates test objects
    val grid = new GridDrawer(worldSize, Size(80, 80))
    val numbers = new GridNumberDrawer(grid)
	val camera = new MagnifierCamera(64)
	
	val handlers = HandlerRelay(actorHandler, drawHandler, mouseEventGen.moveHandler, mouseEventGen.buttonHandler,
		mouseEventGen.wheelHandler)
	
	handlers ++= Vector(grid, numbers, camera, camera.drawHandler)
    camera.drawHandler ++= Vector(grid, numbers)
    
	// Starts the program
	actorLoop.runAsync()
	canvas.startAutoRefresh(Fps(120))
    frame.display()
}