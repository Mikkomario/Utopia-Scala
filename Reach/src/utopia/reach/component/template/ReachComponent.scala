package utopia.reach.component.template

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.handling.mutable.{MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.template.layout.stack.CachingStackable2

/**
  * A common trait for <b>non-wrapping</b> reach components that handle the actual component implementation
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
trait ReachComponent extends ReachComponentLike with CachingStackable2
{
	// ATTRIBUTES	-----------------------
	
	override val boundsPointer = new PointerWithEvents(Bounds.zero)
	override val positionPointer = boundsPointer.map {_.position}
	override val sizePointer = boundsPointer.map {_.size}
	
	override lazy val mouseButtonHandler = MouseButtonStateHandler()
	override lazy val mouseMoveHandler = MouseMoveHandler()
	override lazy val mouseWheelHandler = MouseWheelHandler()
	
	
	// IMPLEMENTED	-----------------------
	
	override def position_=(p: Point) = boundsPointer.update { _.withPosition(p) }
	
	override def size_=(s: Size) = boundsPointer.update { _.withSize(s) }
	
	override def bounds_=(b: Bounds) = boundsPointer.value = b
}
