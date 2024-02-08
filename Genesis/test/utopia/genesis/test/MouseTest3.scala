package utopia.genesis.test

import utopia.flow.async.context.ThreadPool
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.{DrawOrder, DrawSettings, Drawer}
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.handling.action.{ActionLoop, ActorHandler2}
import utopia.genesis.handling.drawing.{Drawable2, RepaintListener}
import utopia.genesis.handling.event.keyboard.{Key, KeyStateListener2, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{MouseEventGenerator2, MouseMoveEvent2, MouseMoveListener2}
import utopia.genesis.handling.template.Handlers
import utopia.genesis.view.AwtCanvas
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import javax.swing.{JFrame, WindowConstants}
import scala.concurrent.ExecutionContext

/**
  * Tests mouse-listening and drawing
  * @author Mikko Hilpinen
  * @since 07/02/2024, v4.0
  */
object MouseTest3 extends App
{
	ParadigmDataType.setup()
	
	implicit val log: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("Genesis-Mouse-Test")
	
	// Creates the GUI
	private val canvas = new AwtCanvas(Size(640, 480))
	private val window = new JFrame()
	
	window.setUndecorated(true)
	window.setLayout(null)
	window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
	window.setContentPane(canvas)
	window.setPreferredSize(canvas.getSize)
	window.setSize(canvas.getSize)
	window.setLocationRelativeTo(null)
	
	// Creates the handlers & event systems
	private val actorHandler = ActorHandler2.empty
	private val actionLoop = new ActionLoop(actorHandler)
	private val mouseGenerator = MouseEventGenerator2(actorHandler, canvas)
	
	private val handlers = Handlers(Vector(actorHandler, canvas.handler) ++ mouseGenerator.handlers)
	
	// Adds the test component
	handlers += TestItem
	KeyboardEvents += KeyStateListener2.key(Key.Esc).pressed { _ => window.dispose() }
	
	// Starts the event-delivery
	actionLoop.runAsync()
	
	// Displays the GUI
	window.setVisible(true)
	
	
	// NESTED   -----------------------
	
	private object TestItem extends Drawable2 with MouseMoveListener2
	{
		// ATTRIBUTES   ---------------
		
		implicit private val ds: DrawSettings = DrawSettings.onlyFill(Color.red)
		
		private val boundsPointer = EventfulPointer(Bounds(Point.origin, Size.square(32)))
		private val shapePointer = boundsPointer.map { b => Circle(b.center, b.size.minDimension / 2.0) }
		
		private var _repaintListeners = Vector.empty[RepaintListener]
		
		
		// IMPLEMENTED  ---------------
		
		override def drawOrder: DrawOrder = DrawOrder.default
		override def opaque: Boolean = false
		
		override def drawBoundsPointer: Changing[Bounds] = boundsPointer.readOnly
		override protected def repaintListeners: Iterable[RepaintListener] = _repaintListeners
		
		override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = drawer.draw(shapePointer.value)
		
		override def addRepaintListener(listener: RepaintListener): Unit = _repaintListeners :+= listener
		override def removeRepaintListener(listener: RepaintListener): Unit =
			_repaintListeners = _repaintListeners.filterNot { _ == listener }
		
		override def onMouseMove(event: MouseMoveEvent2): Unit =
			boundsPointer.update { _.withCenter(event.position) }
	}
}
