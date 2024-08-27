package utopia.genesis.test.interactive

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.{DrawOrder, DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.drawing.AbstractDrawable
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.mouse.{MouseDragEvent, MouseDragListener, MouseWheelEvent, MouseWheelListener}
import utopia.genesis.test.interactive.GenesisTestContext._
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.{Color, Hsl}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.paradigm.transform.Adjustment

/**
  * Tests the enclosing circles -algorithm
  * @author Mikko Hilpinen
  * @since 26.06.2024, v4.0
  */
object EnclosingCircleTest extends App
{
	// ATTRIBUTES   --------------------
	
	private val wheelSizeAdjustment = Adjustment(0.1)
	
	// Creates a number of interactive circles
	val circlePointers = Vector.fill(5) {
		EventfulPointer(Circle(windowSize.toPoint * Vector2D.fill(2) { 0.1 + math.random() * 0.8 },
			24 + math.random() * 128))
	}
	handlers ++= circlePointers.map { new CircleDrawer(_, Hsl(Angle.random), DrawOrder.default + 1) }
	handlers ++= circlePointers.map { new InteractiveCircle(_) }
	
	// Creates a circle which always encloses the others
	val enclosingCirclePointer = EventfulPointer(Circle.zero)
	circlePointers.foreach { _.addAnyChangeListener {
		enclosingCirclePointer.value = Circle.enclosing(circlePointers.map { _.value.origin })
		// enclosingCirclePointer.value = Circle.enclosingCircles(circlePointers.map { _.value }, 0.25)
	} }
	handlers += new CircleDrawer(enclosingCirclePointer, Hsl(Angle.random, saturation = 0.5))
	
	start()
	
	
	// NESTED   ------------------------
	
	// Enables drawing for a circle
	private class CircleDrawer(circleView: Changing[Circle], color: Color, drawOrder: DrawOrder = DrawOrder.default)
		extends AbstractDrawable(drawOrder)
	{
		// ATTRIBUTES   ----------------
		
		private implicit val ds: DrawSettings = DrawSettings(color)(StrokeSettings(color.darkened, 2.0))
		
		override val drawBoundsPointer: Changing[Bounds] = circleView.map { _.bounds.ceil }
		
		
		// IMPLEMENTED  ----------------
		
		override def handleCondition: Flag = AlwaysTrue
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = drawer.draw(Circle.within(bounds))
	}
	
	// Enables mouse-interactions for a circle
	private class InteractiveCircle(pointer: EventfulPointer[Circle]) extends MouseDragListener with MouseWheelListener
	{
		// ATTRIBUTES   ----------------
		
		override val mouseWheelEventFilter: Filter[MouseWheelEvent] = MouseWheelEvent.filter.over(circle)
		
		private var relativeDragPosition: Option[DoubleVector] = None
		
		
		// COMPUTED --------------------
		
		private def circle = pointer.value
		
		private def origin = circle.origin
		private def origin_=(newOrigin: Point) = pointer.update { _.withOrigin(newOrigin) }
		
		
		// IMPLEMENTED  ----------------
		
		override def handleCondition: Flag = AlwaysTrue
		override def mouseDragEventFilter: Filter[MouseDragEvent] = AcceptAll
		
		// On mouse drag, follows the mouse
		override def onMouseDrag(event: MouseDragEvent): Unit = {
			if (event.isDragStart) {
				if (circle.contains(event.dragOrigin))
					relativeDragPosition = Some(event.dragOrigin - origin)
			}
			if (event.isDragEnd)
				relativeDragPosition = None
			else
				relativeDragPosition.foreach { p => origin = event.position - p }
		}
		
		// On mouse wheel, increases or decreases size
		override def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice = pointer.update { _.mapRadius { r =>
			(r * wheelSizeAdjustment(event.up)) max 24
		} }
	}
}
