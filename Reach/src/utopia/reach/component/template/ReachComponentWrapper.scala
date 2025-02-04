package utopia.reach.component.template

import utopia.firmament.component.stack.StackableWrapper
import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.genesis.handling.event.mouse.MouseDragHandler
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D

/**
  * A common trait for Reach component wrapper classes
  * @author Mikko Hilpinen
  * @since 24.10.2020, v0.1
  */
trait ReachComponentWrapper extends ReachComponent with StackableWrapper
{
	// ABSTRACT	--------------------------------
	
	override protected def wrapped: ReachComponent
	
	
	// IMPLEMENTED	----------------------------
	
	override def hierarchy = wrapped.hierarchy
	override def children = wrapped.children
	
	override def transparent = wrapped.transparent
	
	override def positionPointer = wrapped.positionPointer
	override def sizePointer = wrapped.sizePointer
	override def boundsPointer = wrapped.boundsPointer
	
	override def mouseDragHandler: MouseDragHandler = wrapped.mouseDragHandler
	
	override def toImage = wrapped.toImage
	
	override def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) =
		wrapped.paintContent(drawer, drawLevel, clipZone)
	
	override def paintMovement(movement: => Vector2D) = wrapped.paintMovement(movement)
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = wrapped.paintWith(drawer, clipZone)
	override def regionToImage(region: Bounds) = wrapped.regionToImage(region)
}
