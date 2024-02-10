package utopia.genesis.test.interactive

import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.event.MouseButton
import utopia.genesis.graphics.{DrawOrder, DrawSettings, Drawer}
import utopia.genesis.handling.action.{ActionLoop, ActorHandler2}
import utopia.genesis.handling.drawing.{Drawable2, RepaintListener}
import utopia.genesis.handling.event.ConsumeChoice
import utopia.genesis.handling.event.keyboard.KeyStateListener2.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent2, KeyStateListener2, KeyboardEvents}
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
	private val actorHandler = ActorHandler2.empty
	private val actionLoop = new ActionLoop(actorHandler)
	private val mouseGenerator = MouseEventGenerator2(actorHandler, canvas)
	
	private val handlers = Handlers(Vector(actorHandler, canvas.handler) ++ mouseGenerator.handlers)
	
	// Adds the test component
	handlers += TestItem
	KeyboardEvents += KeyStateListener2.apply(Key.Esc).pressed { _ => window.dispose() }
	
	// Starts the event-delivery
	actionLoop.runAsync()
	
	KeyboardEvents += KeyListener
	
	// Displays the GUI
	window.setVisible(true)
	
	
	// NESTED   -----------------------
	
	private object KeyListener extends KeyStateListener2
	{
		override def onKeyState(event: KeyStateEvent2): Unit = println(event)
		override def keyStateEventFilter: KeyStateEventFilter = AcceptAll
		override def handleCondition: FlagLike = AlwaysTrue
	}
	
	private object TestItem
		extends Drawable2 with MouseMoveListener2 with MouseWheelListener2 with MouseButtonStateListener2
	{
		// ATTRIBUTES   ---------------
		
		private val radiusAdjustment = Adjustment(0.1)
		override val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2] =
			MouseButtonStateEvent2.filter(MouseButton.Left, MouseButton.Right)
		override val mouseMoveEventFilter: Filter[MouseMoveEvent2] = !MouseMoveEvent2.filter.whileRightDown
		
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
		
		private var _repaintListeners = Vector.empty[RepaintListener]
		
		
		// COMPUTED -------------------
		
		implicit private def ds: DrawSettings = DrawSettings.onlyFill(colorPointer.value)
		
		
		// IMPLEMENTED  ---------------
		
		override def drawOrder: DrawOrder = DrawOrder.default
		override def opaque: Boolean = false
		
		override def drawBoundsPointer: Changing[Bounds] = boundsPointer.readOnly
		override protected def repaintListeners: Iterable[RepaintListener] = _repaintListeners
		
		override def mouseWheelEventFilter: Filter[MouseWheelEvent2] = AcceptAll
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = drawer.draw(shapeCache(bounds))
		
		override def addRepaintListener(listener: RepaintListener): Unit = _repaintListeners :+= listener
		override def removeRepaintListener(listener: RepaintListener): Unit =
			_repaintListeners = _repaintListeners.filterNot { _ == listener }
		
		override def onMouseMove(event: MouseMoveEvent2): Unit = {
			if (event.buttonStates.left)
				colorAnglePointer.update { _ + Rotation.circles(0.001).clockwise * event.transition.length }
			positionPointer.value = event.position
		}
		override def onMouseWheelRotated(event: MouseWheelEvent2): ConsumeChoice =
			radiusPointer.update { _ * radiusAdjustment(-event.wheelTurn) }
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice = {
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
