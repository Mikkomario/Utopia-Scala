package utopia.genesis.test

import utopia.flow.test.TestContext._
import utopia.genesis.graphics.{DrawSettings, Drawer3}
import utopia.genesis.handling.{ActorLoop, Drawable2}
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler2}
import utopia.genesis.view.{Canvas2, CanvasMouseEventGenerator2, MainFrame}
import utopia.inception.handling.HandlerType
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.paradigm.shape.template.Dimensions

/**
  * Tests color highlighting
  * @author Mikko Hilpinen
  * @since 30.11.2022, v3.1.1
  */
object ColorHighlightTest extends App
{
	ParadigmDataType.setup()
	
	private val highlightUnit = 0.1
	private val highlightCount = 6
	// private val highlightSteps = Iterator.iterate(highlightUnit) { _ + highlightUnit }.take(highlightCount).toVector
	
	private val unit = 48
	private val unitSize = Size.square(unit)
	
	println(Color.white.relativeLuminance)
	
	private val startColors = Vector(
		Color.red, Color.blue, Color.green, Color.yellow, Color.magenta,
		Color.black, Color.white, Color.gray(0.2), Color.gray(0.8), Color.gray(0.5),
		Color.red.withLuminosity(0.25), Color.red.withLuminosity(0.75),
		Color.blue.withSaturation(0.5), Color.blue.withSaturation(0.5).withLuminosity(0.8)
	)
	
	class ColorDrawer(color: Color, pos: Dimensions[Double]) extends Drawable2
	{
		private implicit val drawSettings: DrawSettings = DrawSettings.onlyFill(color)
		private val shape = Bounds(Point(unit, unit) * pos, unitSize)
		
		override def draw(drawer: Drawer3) = drawer.draw(shape)
		
		override def allowsHandlingFrom(handlerType: HandlerType) = true
	}
	
	// TODO: These should be accessed through a test context
	
	// Creates the handlers
	val gameWorldSize = Size((highlightCount * 2 + 1) * unit, unit * startColors.size)
	
	val drawHandler = DrawableHandler2()
	val actorHandler = ActorHandler()
	
	val canvas = new Canvas2(drawHandler, gameWorldSize)
	val mouseEventGen = new CanvasMouseEventGenerator2(canvas)
	
	val handlers = HandlerRelay(drawHandler, actorHandler, mouseEventGen.buttonHandler, mouseEventGen.moveHandler,
		mouseEventGen.wheelHandler)
	
	// Creates event generators
	val actorLoop = new ActorLoop(actorHandler, 10 to 120)
	
	// Creates test objects
	val rowIndexIter = Iterator.iterate(0) { _ + 1 }
	def addRow(color: Color) = {
		val rowIndex = rowIndexIter.next()
		val lights = (1 to highlightCount).map { color.lightenedBy(_) }
		// highlightSteps.map { h => color.lightened(1 + h) }
		val darks = (1 to highlightCount).map { color.darkenedBy(_) }
		// highlightSteps.map { h => color.darkened(1 + h) }
		val drawers = (darks.reverse.zipWithIndex.map { case (color, index) =>
			new ColorDrawer(color, Dimensions.double(index, rowIndex))
		} :+ new ColorDrawer(color, Dimensions.double(highlightCount, rowIndex))) ++
			lights.zipWithIndex.map { case (color, index) =>
				new ColorDrawer(color, Dimensions.double(highlightCount + 1 + index, rowIndex))
			}
		handlers ++= drawers
	}
	startColors.foreach(addRow)
	
	// Creates the frame
	val frame = new MainFrame(canvas, gameWorldSize, "Color Highlight Test")
	
	actorHandler += mouseEventGen
	
	// Displays the frame
	actorLoop.runAsync()
	canvas.startAutoRefresh()
	
	frame.display()
}
