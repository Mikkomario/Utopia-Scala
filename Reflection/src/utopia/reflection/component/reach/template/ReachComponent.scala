package utopia.reflection.component.reach.template

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.handling.mutable.{MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.template.layout.stack.CachingStackable2

/**
  * A common trait for non-wrapping reach components that handle the actual component implementatio
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
trait ReachComponent extends ReachComponentLike with CachingStackable2
{
	// ATTRIBUTES	-----------------------
	
	val boundsPointer = new PointerWithEvents(Bounds.zero)
	val positionPointer = boundsPointer.map {_.position}
	val sizePointer = boundsPointer.map {_.size}
	
	lazy val mouseButtonHandler = MouseButtonStateHandler()
	lazy val mouseMoveHandler = MouseMoveHandler()
	lazy val mouseWheelHandler = MouseWheelHandler()
	
	
	// IMPLEMENTED	-----------------------
	
	override def position_=(p: Point) = boundsPointer.update {_.withPosition(p)}
	
	override def size_=(s: Size) = boundsPointer.update {_.withSize(s)}
	
	override def bounds_=(b: Bounds) = boundsPointer.value = bounds
}
