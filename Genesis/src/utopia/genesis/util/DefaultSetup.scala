package utopia.genesis.util

import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler, KeyStateHandler, KeyTypedHandler}
import utopia.genesis.view.{Canvas, CanvasMouseEventGenerator, GlobalKeyboardEventHandler, GlobalMouseEventHandler, MainFrame}
import utopia.inception.handling.mutable.HandlerRelay
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.vector.size.Size

import scala.concurrent.ExecutionContext

/**
  * This class provides a default configuration for Genesis-dependent projects
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
class DefaultSetup(initialGameWorldSize: Size, title: String, val maxFPS: Fps = Fps.default)
				  (implicit context: ExecutionContext, logger: Logger)
	extends Setup
{
	// ATTRIBUTES	--------------------
	
	private val started = new VolatileFlag()
	
	// Handlers
	/**
	  * Handler for actors
	  */
	val actorHandler = ActorHandler()
	/**
	  * Handler for drawable items
	  */
	val drawHandler = DrawableHandler()
	/**
	  * Handler for key state events
	  */
	val keyStateHandler = KeyStateHandler()
	/**
	  * Handler for key typed events
	  */
	val keyTypedHandler = KeyTypedHandler()
	
	/**
	  * The handler relay for this setup
	  */
	override lazy val handlers = HandlerRelay(actorHandler, drawHandler, keyStateHandler, keyTypedHandler,
		mouseButtonHandler, mouseMoveHandler, mouseWheelHandler)
	
	// View
	/**
	  * The canvas that displays graphics
	  */
	val canvas = new Canvas(drawHandler, initialGameWorldSize)
	/**
	  * The frame the canvas is displayed in
	  */
	val frame = new MainFrame(canvas, initialGameWorldSize, title)
	
	// Generators
	private val actorLoop = new ActorLoop(actorHandler, 15 to maxFPS.fps)
	private val mouseEventGenerator = new CanvasMouseEventGenerator(canvas)
	
	
	// INITIAL CODE	-------------------
	
	// Sets up data types, if not already
	ParadigmDataType.setup()
	
	// Registers generators
	actorHandler += mouseEventGenerator
	GlobalKeyboardEventHandler.specifyExecutionContext(context)
	
	
	// COMPUTED	-----------------------
	
	/**
	  * Handler for mouse button state events
	  */
	def mouseButtonHandler = mouseEventGenerator.buttonHandler
	/**
	  * Handler for mouse move events
	  */
	def mouseMoveHandler = mouseEventGenerator.moveHandler
	/**
	  * Handler for mouse wheel events
	  */
	def mouseWheelHandler = mouseEventGenerator.wheelHandler
	
	/**
	  * @return The current game world size
	  */
	def gameWorldSize = canvas.gameWorldSize
	
	
	// IMPLEMENTED	-----------------------
	
	/**
	  * Starts the program (only works once)
	  */
	override def start() = {
		if (started.set()) {
			GlobalMouseEventHandler.registerGenerator(mouseEventGenerator)
			
			GlobalKeyboardEventHandler += keyStateHandler
			GlobalKeyboardEventHandler += keyTypedHandler
			
			actorLoop.runAsync()
			canvas.startAutoRefresh(maxFPS)
			
			frame.setLocationRelativeTo(null)
			frame.display()
		}
	}
}
