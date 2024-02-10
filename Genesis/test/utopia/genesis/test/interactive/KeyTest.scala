package utopia.genesis.test.interactive

import utopia.flow.test.TestContext._
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.{Drawable, KeyStateListener, KeyTypedListener}
import utopia.genesis.util.DefaultSetup
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.awt.Color
import java.awt.event.KeyEvent

/**
 * This is an interactive test for keyboard interactions. The square character should move according
 * to singular key presses (one square per press). Any characters typed on the keyboard should
 * appear on the console.
 * @author Mikko Hilpinen
 * @since 22.2.2017
 */
object KeyTest extends App
{
    private class TestObject(startPosition: Point) extends KeyStateListener with Handleable
    {
        // ATTRIBUTES    -----------------
        
        private var _position = startPosition
        def position = _position
        
        // Is only interested in key presses
        override val keyStateEventFilter = KeyStateEvent.wasPressedFilter
        
        
        // IMPLEMENTED METHODS    --------
        
        override def onKeyState(event: KeyStateEvent) = 
        {
            event.index match
            {
                case KeyEvent.VK_UP => _position += Y(-1)
                case KeyEvent.VK_RIGHT => _position += X(1)
                case KeyEvent.VK_DOWN => _position += Y(1)
                case KeyEvent.VK_LEFT => _position += X(-1)
                case _ => 
            }
        }
    }
    
    private class View(private val testObj: TestObject, private val gameWorldSize: Size,
			   private val squareSide: Int) extends Drawable with Handleable
    {
        // ATTRIBUTES    -----------------
        
        implicit val ss: StrokeSettings = StrokeSettings(Color.lightGray)
        private val objDs = DrawSettings(Color.darkGray)
        
        private val gridSquares = Size(gameWorldSize.width.toInt / squareSide, gameWorldSize.height.toInt / squareSide)
        private val gridSize = gridSquares * squareSide
        private val gridPosition = ((gameWorldSize - gridSize) / 2).toPoint
		
        private val squareSize = Size(squareSide, squareSide)
        private val avatarSize = squareSize * 0.8
        
        def draw(drawer: Drawer) =
        {
            // Draws the grid first
            for (x <- 0 to gridSquares.width.toInt) {
                drawer.draw(Line.fromVector(gridPosition + X(squareSide * x), gridSize.yProjection.toVector))(StrokeSettings.default)
            }
            for (y <- 0 to gridSquares.height.toInt) {
                drawer.draw(Line.fromVector(gridPosition + Y(squareSide * y), gridSize.xProjection.toVector))(StrokeSettings.default)
            }
            
            // Then draws the object
            drawer.draw(Bounds(gridPosition + testObj.position * squareSide +
                (squareSize - avatarSize) / 2, avatarSize).toRoundedRectangle())(objDs)
        }
    }
    
    private class KeyTypePrinter extends KeyTypedListener with Handleable
    {
        override def onKeyTyped(event: KeyTypedEvent) = print(event.typedChar)
    }
	
	// Sets up the program
	val gameWorldSize = Size(800, 600)
	
	val setup = new DefaultSetup(gameWorldSize, "Key Test")
	
	private val testObj = new TestObject(Point(3, 2))
	private val view = new View(testObj, gameWorldSize, 48)
	
	setup.registerObjects(testObj, view, new KeyTypePrinter())
	
	// Starts the program
	setup.start()
}