package utopia.genesis.test.interactive

import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.Priority.Normal
import utopia.genesis.graphics.{DrawOrder, DrawSettings, Drawer}
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.handling.drawing.{Drawable, DrawableHandler, RepaintListener, Repositioner}
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.Handlers
import utopia.genesis.view.{AwtCanvas, MainFrame}
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.color.{Color, Hsl}
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.transform.Adjustment

import scala.concurrent.ExecutionContext

/**
  * Sets up context for interactive tests
  * @author Mikko Hilpinen
  * @since 07/02/2024, v4.0
  */
object GenesisTestContext
{
	ParadigmDataType.setup()
	
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Genesis")
	
	// Creates the GUI
	// Applies clipping at the edges
	/**
	  * Handler which facilitates window-level drawing
	  */
	val drawableHandler = DrawableHandler.withFpsLimit(90, Normal).empty
	/**
	  * Size of the window used
	  */
	val windowSize = Size(640, 480)
	private val canvas = new AwtCanvas(windowSize, drawableHandler)
	private val window = new MainFrame(canvas, Size(640, 480), "Test", borderless = true)
	
	window.setLocationRelativeTo(null)
	window.display()
	
	// Creates the handlers & event systems
	/**
	  * Handler used for distributing the action events
	  */
	val actorHandler = ActorHandler.empty
	private val actionLoop = new ActionLoop(actorHandler)
	private val mouseGenerator = MouseEventGenerator(actorHandler, canvas)
	private val dragHandler = MouseDragHandler.empty
	
	/**
	  * Handles used for distributing events.
	  * Contains: ActorHandler, DrawableHandler and mouse handlers
	  */
	val handlers = Handlers(Vector(actorHandler, canvas.handler, dragHandler) ++ mouseGenerator.handlers)
	
	/**
	  * Starts the test
	  */
	def start() = {
		// Starts the event-delivery
		actionLoop.runAsync()
		
		// Sets up remaining events & listeners
		KeyboardEvents += KeyListener
		handlers += new DragTracker(dragHandler)
		
		// Closes the window with esc
		KeyboardEvents += KeyStateListener.apply(Key.Esc).pressed { _ => window.dispose() }
		
		// Displays the GUI
		window.setVisible(true)
	}
	
	
	// NESTED   -----------------------
	
	private object KeyListener extends KeyStateListener
	{
		override def onKeyState(event: KeyStateEvent): Unit = println(event)
		override def keyStateEventFilter: KeyStateEventFilter = AcceptAll
		override def handleCondition: Flag = AlwaysTrue
	}
}
