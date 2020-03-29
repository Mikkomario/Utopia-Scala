package utopia.genesis.util

import utopia.flow.async.VolatileFlag
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.{ActorHandler, DrawableHandler, KeyStateHandler, KeyTypedHandler, MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.view.{Canvas, CanvasMouseEventGenerator, ConvertingKeyListener, MainFrame}
import utopia.inception.handling.mutable.HandlerRelay

import scala.concurrent.ExecutionContext

/**
  * This class provides a default configuration for Genesis-dependent projects
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
class DefaultSetup(initialGameWorldSize: Size, title: String, val maxFPS: FPS = FPS.default) extends Setup
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
	  * Handler for mouse button state events
	  */
	val mouseButtonHandler = MouseButtonStateHandler()
	/**
	  * Handler for mouse move events
	  */
	val mouseMoveHandler = MouseMoveHandler()
	/**
	  * Handler for mouse wheel events
	  */
	val mouseWheelHandler = MouseWheelHandler()
	
	/**
	  * The handler relay for this setup
	  */
	override val handlers = HandlerRelay(actorHandler, drawHandler, keyStateHandler, keyTypedHandler, mouseButtonHandler,
		mouseMoveHandler, mouseWheelHandler)
	
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
	private val mouseEventGenerator = new CanvasMouseEventGenerator(canvas, mouseMoveHandler, mouseButtonHandler, mouseWheelHandler)
	
	
	// INITIAL CODE	-------------------
	
	// Sets up datatypes, if not already
	GenesisDataType.setup()
	
	// Registers generators
	actorHandler += mouseEventGenerator
	new ConvertingKeyListener(keyStateHandler, keyTypedHandler).register()
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return The current game world size
	  */
	def gameWorldSize = canvas.gameWorldSize
	
	
	// IMPLEMENTED	-----------------------
	
	/**
	  * Starts the program (only works once)
	  * @param context The execution context for asynchronous operations (implicit).
	  *                You can use utopia.flow.async.ThreadPool, for example
	  */
	override def start()(implicit context: ExecutionContext) =
	{
		started.runAndSet
		{
			actorLoop.startAsync()
			actorLoop.registerToStopOnceJVMCloses()
			
			canvas.startAutoRefresh(maxFPS)
			
			frame.display()
		}
	}
}
