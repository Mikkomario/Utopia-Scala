package utopia.genesis.test

import utopia.genesis.view.Canvas
import utopia.genesis.view.MainFrame
import java.awt.Font

import utopia.flow.async.ThreadPool
import utopia.genesis.util.{Drawer, FPS}
import utopia.genesis.shape.shape2D.{Bounds, Size}
import utopia.genesis.view.CanvasMouseEventGenerator
import utopia.genesis.handling.{ActorLoop, Drawable}
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler, MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.inception.handling.immutable.Handleable
import utopia.inception.handling.mutable.HandlerRelay

import scala.concurrent.ExecutionContext


object CameraTest extends App
{
    class GridNumberDrawer(private val grid: GridDrawer) extends Drawable with Handleable
    {
        private val font = new Font("Arial", 0, 14)
        
        override def draw(drawer: Drawer) = 
        {
            // Draws a number on each grid square
            for (x <- 0 until grid.squareAmounts.x.toInt; y <- 0 until grid.squareAmounts.y.toInt)
            {
                drawer.drawTextCentered((y * grid.squareAmounts.x.toInt + x + 1) + "", font, 
                        Bounds(grid.squarePosition(x, y), grid.squareSize))
            }
        }
    }
	
	// Creates handlers
	val actorHandler = ActorHandler()
	val mouseMoveHandler = MouseMoveHandler()
	val mouseButtonHandler = MouseButtonStateHandler()
	val mouseWheelHandler = MouseWheelHandler()
	val drawHandler = DrawableHandler()
	
	val handlers = HandlerRelay(actorHandler, mouseMoveHandler, mouseButtonHandler, mouseWheelHandler, drawHandler)
	
	// Creates frame
    val worldSize = Size(800, 600)
    
    val canvas = new Canvas(drawHandler, worldSize)
    val frame = new MainFrame(canvas, worldSize, "Camera Test")
    
	// Sets up generators
    val actorLoop = new ActorLoop(actorHandler, 20 to 120)
    val mouseEventGen = new CanvasMouseEventGenerator(canvas, mouseMoveHandler, mouseButtonHandler, mouseWheelHandler)
    actorHandler += mouseEventGen
    
	// Creates test objects
    val grid = new GridDrawer(worldSize, Size(80, 80))
    val numbers = new GridNumberDrawer(grid)
	val camera = new MagnifierCamera(64)
 
	handlers ++= (grid, numbers, camera, camera.drawHandler)
    camera.drawHandler ++= (grid, numbers)
    
	// Starts the program
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	
	actorLoop.startAsync()
	canvas.startAutoRefresh(FPS(120))
    frame.display()
}