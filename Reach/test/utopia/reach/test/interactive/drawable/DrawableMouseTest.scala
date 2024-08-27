package utopia.reach.test.interactive.drawable

import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Foreground
import utopia.genesis.graphics.{DrawOrder, DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.drawing.{AbstractDrawable, Drawable, RepaintListener}
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.mouse._
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.color.{Color, Hsl}
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.transform.Adjustment

/**
  * Tests mouse-listening and drawable canvas in Reach context
  * @author Mikko Hilpinen
  * @since 25/02/2024, v1.3
  */
object DrawableMouseTest extends App
{
	import utopia.reach.test.ReachTestContext._
	import DrawableReachTestContext._
	
	private val viewBoundsPointer = Fixed(viewBounds)
	
	println(canvas.stackSize)
	
	window.display(centerOnParent = true)
	display(TestItem, DrawBoundsDrawer)
	
	private object DrawBoundsDrawer extends AbstractDrawable(Foreground)
	{
		// ATTRIBUTES   ---------------
		
		private implicit val ds: DrawSettings = StrokeSettings(Color.blue, 3.0)
		
		private val shapePointer = viewBoundsPointer.map { _.shrunk(4) }
		
		
		// IMPLEMENTED  ---------------
		
		override def handleCondition: Flag = AlwaysTrue
		
		override def drawBoundsPointer: Changing[Bounds] = shapePointer
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = drawer.draw(bounds)
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
		
		private var _repaintListeners = Vector.empty[RepaintListener]
		
		
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
