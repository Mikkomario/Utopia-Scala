package utopia.reflection.component.reach.template

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.handling.mutable.{MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.template.layout.stack.CachingStackable2

/**
  * A common trait for <b>non-wrapping</b> reach components that handle the actual component implementation
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
	
	
	// INITIAL CODE	-----------------------
	
	// Whenever component bounds update, repaints the affected area
	boundsPointer.addListener { event =>
		// Case: Component size changed => repaints affected area (including both old and new bounds)
		// TODO: Fix copy issue
		// TODO: Also, leaves some of the old area unpainted (like one pixel to the right)
		/*
		if (event.compareBy { _.size })*/
			Bounds.aroundOption(Vector(event.oldValue, event.newValue).filter { _.size.isPositive }/*.map { _.ceil }*/)
				.foreach { parentHierarchy.repaint(_) }
		// Case: Only position changed => shifts this component, copying already painted region
		/*else if (event.newValue.size.isPositive)
			paintMovement((event.newValue.position - event.oldValue.position).toVector)*/
	}
	
	
	// IMPLEMENTED	-----------------------
	
	override def position_=(p: Point) = boundsPointer.update { _.withPosition(p) }
	
	override def size_=(s: Size) = boundsPointer.update { _.withSize(s) }
	
	override def bounds_=(b: Bounds) = boundsPointer.value = b
}
