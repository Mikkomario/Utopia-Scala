package utopia.genesis.test.interactive

import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.Empty
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
	// Applies clipping at the edges
	private val canvas = new AwtCanvas(Size(640, 480),
		DrawableHandler.clippedTo(Bounds(20, 20, 600, 440)).withFpsLimit(90, Normal).empty)
	private val window = new MainFrame(canvas, Size(640, 480), "Test", borderless = true)
	
	/*
	window.setUndecorated(true)
	window.setLayout(null)
	window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
	window.setContentPane(canvas)
	window.setPreferredSize(canvas.getSize)
	window.setSize(canvas.getSize)
	
	 */
	window.setLocationRelativeTo(null)
	window.display()
	
	// Creates the handlers & event systems
	private val actorHandler = ActorHandler.empty
	private val actionLoop = new ActionLoop(actorHandler)
	private val mouseGenerator = MouseEventGenerator(actorHandler, canvas)
	
	private val handlers = Handlers(Vector(actorHandler, canvas.handler) ++ mouseGenerator.handlers)
	
	// Adds the test component
	handlers += TestItem
	KeyboardEvents += KeyStateListener.apply(Key.Esc).pressed { _ => window.dispose() }
	
	// Adds a view to the test component
	private val repositioner = new Repositioner(TestItem, Left(Fixed(Point(200, 200)), Fixed(Size(20, 20))))
	handlers += repositioner
	private val repositionedHandlers = repositioner.setupMouseEvents(handlers, disableMouseToWrapped = true)
	repositionedHandlers += MouseMoveListener.apply { e => println(e.position.relative) }
	
	// Starts the event-delivery
	actionLoop.runAsync()
	
	KeyboardEvents += KeyListener
	
	// Displays the GUI
	window.setVisible(true)
	
	
	// NESTED   -----------------------
	
	private object KeyListener extends KeyStateListener
	{
		override def onKeyState(event: KeyStateEvent): Unit = println(event)
		override def keyStateEventFilter: KeyStateEventFilter = AcceptAll
		override def handleCondition: Flag = AlwaysTrue
	}
	
	private object TestItem
		extends Drawable with MouseMoveListener with MouseWheelListener with MouseButtonStateListener
	{
		// ATTRIBUTES   ---------------
		
		private val radiusAdjustment = Adjustment(0.1)
		override val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] =
			MouseButtonStateEvent.filter(MouseButton.Left, MouseButton.Right)
		override val mouseMoveEventFilter: Filter[MouseMoveEvent] = !MouseMoveEvent.filter.whileRightDown
		
		private val radiusPointer = EventfulPointer(32.0)
		private val positionPointer = EventfulPointer(Point.origin)
		private val colorAnglePointer = EventfulPointer(Angle.zero)
		
		private val boundsPointer = positionPointer.mergeWith(radiusPointer) { (p, r) =>
			Bounds.fromFunction2D { axis =>
				val center = p(axis)
				NumericSpan[Double]((center - r).round.toDouble, (center + r).round.toDouble)
			}
		}
		private val colorPointer = colorAnglePointer.map[Color] { Hsl(_) }
		private val shapeCache = Cache.onlyLatest { b: Bounds =>
			Circle(b.center, b.size.minDimension / 2.0)
		}
		
		private var _repaintListeners: Seq[RepaintListener] = Empty
		
		
		// COMPUTED -------------------
		
		implicit private def ds: DrawSettings = DrawSettings.onlyFill(colorPointer.value)
		
		
		// IMPLEMENTED  ---------------
		
		override def drawOrder: DrawOrder = DrawOrder.default
		override def opaque: Boolean = false
		
		override def drawBoundsPointer: Changing[Bounds] = boundsPointer.readOnly
		override def repaintListeners: Iterable[RepaintListener] = _repaintListeners
		
		override def mouseWheelEventFilter: Filter[MouseWheelEvent] = AcceptAll
		override def handleCondition: Flag = AlwaysTrue
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = drawer.draw(shapeCache(bounds))
		
		override def addRepaintListener(listener: RepaintListener): Unit = _repaintListeners :+= listener
		override def removeRepaintListener(listener: RepaintListener): Unit =
			_repaintListeners = _repaintListeners.filterNot { _ == listener }
		
		override def onMouseMove(event: MouseMoveEvent): Unit = {
			if (event.buttonStates.left)
				colorAnglePointer.update { _ + Rotation.circles(0.001).clockwise * event.transition.length }
			positionPointer.value = event.position
		}
		override def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice =
			radiusPointer.update { _ * radiusAdjustment(-event.wheelTurn) }
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = {
			val dir = event.button match {
				case MouseButton.Left => Clockwise
				case MouseButton.Right => Counterclockwise
				case _ => Clockwise
			}
			val amount = if (event.pressed) Rotation.circles(0.2) else Rotation.circles(0.1)
			colorAnglePointer.update { _ + amount.towards(dir) }
			repaint()
		}
	}
}
