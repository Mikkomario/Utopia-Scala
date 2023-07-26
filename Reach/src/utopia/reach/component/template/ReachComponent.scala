package utopia.reach.component.template

import utopia.firmament.component.stack.CachingStackable
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.mutable.{MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}

/**
  * A common trait for <b>non-wrapping</b> reach components that handle the actual component implementation
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
abstract class ReachComponent extends ReachComponentLike with CachingStackable
{
	// ATTRIBUTES	-----------------------
	
	override val boundsPointer = new EventfulPointer(Bounds.zero)
	override lazy val positionPointer = boundsPointer.lightMap { _.position }
	override lazy val sizePointer = boundsPointer.lightMap { _.size }
	
	override lazy val mouseButtonHandler = MouseButtonStateHandler()
	override lazy val mouseMoveHandler = MouseMoveHandler()
	override lazy val mouseWheelHandler = MouseWheelHandler()
	
	
	// IMPLEMENTED	-----------------------
	
	override def position_=(p: Point) = boundsPointer.update { _.withPosition(p) }
	override def size_=(s: Size) = boundsPointer.update { _.withSize(s) }
	override def bounds_=(b: Bounds) = boundsPointer.value = b
}
