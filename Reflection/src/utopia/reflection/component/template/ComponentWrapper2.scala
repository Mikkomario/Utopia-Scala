package utopia.reflection.component.template

import utopia.paradigm.shape.shape2d.{Point, Size}
import utopia.reflection.text.Font

/**
  * These wrappers look like components, but only wrap another component
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1
  */
trait ComponentWrapper2 extends ComponentLike2
{
	// ABSTRACT	-------------------
	
	/**
	  * @return The component wrapped by this wrapper
	  */
	protected def wrapped: ComponentLike2
	
	
	// IMPLEMENTED	---------------
	
	override def fontMetricsWith(font: Font) = wrapped.fontMetricsWith(font)
	
	override def mouseButtonHandler = wrapped.mouseButtonHandler
	override def mouseMoveHandler = wrapped.mouseMoveHandler
	override def mouseWheelHandler = wrapped.mouseWheelHandler
	
	override def position = wrapped.position
	override def position_=(p: Point) = wrapped.position = p
	
	override def size = wrapped.size
	override def size_=(s: Size) = wrapped.size = s
	
	override def children = wrapped.children
}
