package utopia.reach.component.template

import utopia.firmament.component.stack.CachingStackable
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.event.mouse.{MouseButtonStateHandler, MouseDragHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.handling.template.Handlers
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * A common trait for <b>non-wrapping</b> reach components that handle the actual component implementation
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
trait ConcreteReachComponent extends ReachComponent with CachingStackable
{
	// ATTRIBUTES	-----------------------
	
	override val boundsPointer = EventfulPointer(Bounds.zero)
	override lazy val positionPointer = boundsPointer.lightMap { _.position }
	override lazy val sizePointer = boundsPointer.lightMap { _.size }
	/**
	  * A pointer that contains the current width of this component
	  */
	lazy val widthPointer = sizePointer.lightMap { _.width }
	/**
	  * A pointer that contains the current height of this component
	  */
	lazy val heightPointer = sizePointer.lightMap { _.height }
	
	override lazy val mouseButtonHandler = MouseButtonStateHandler()
	override lazy val mouseMoveHandler = MouseMoveHandler()
	override lazy val mouseWheelHandler = MouseWheelHandler()
	override lazy val mouseDragHandler: MouseDragHandler = MouseDragHandler()
	
	override lazy val handlers: Handlers =
		Handlers(mouseButtonHandler, mouseMoveHandler, mouseWheelHandler, mouseDragHandler)
	
	
	// IMPLEMENTED	-----------------------
	
	override def position_=(p: Point) = boundsPointer.update { _.withPosition(p) }
	override def size_=(s: Size) = boundsPointer.update { _.withSize(s) }
	override def bounds_=(b: Bounds) = boundsPointer.value = b
}
