package utopia.firmament.component

import utopia.genesis.handling.template.Handlers
import utopia.genesis.text.Font
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * These wrappers look like components, but only wrap another component
  * @author Mikko Hilpinen
  * @since 28.4.2019, Reflection v1
  */
trait ComponentWrapper extends Component
{
	// ABSTRACT	-------------------
	
	/**
	  * @return The component wrapped by this wrapper
	  */
	protected def wrapped: Component
	
	
	// IMPLEMENTED	---------------
	
	override def fontMetricsWith(font: Font) = wrapped.fontMetricsWith(font)
	
	override def mouseButtonHandler = wrapped.mouseButtonHandler
	override def mouseMoveHandler = wrapped.mouseMoveHandler
	override def mouseWheelHandler = wrapped.mouseWheelHandler
	
	override def handlers: Handlers = wrapped.handlers
	
	override def position = wrapped.position
	override def position_=(p: Point) = wrapped.position = p
	
	override def size = wrapped.size
	override def size_=(s: Size) = wrapped.size = s
	
	override def bounds: Bounds = wrapped.bounds
	override def bounds_=(b: Bounds): Unit = wrapped.bounds = b
	
	override def children = wrapped.children
}
