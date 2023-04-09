package utopia.reach.component.template

import utopia.genesis.graphics.Drawer3
import utopia.paradigm.shape.shape2d.{Bounds, Vector2D}
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.template.layout.stack.StackableWrapper2

/**
  * A common trait for Reach component wrapper classes
  * @author Mikko Hilpinen
  * @since 24.10.2020, v0.1
  */
trait ReachComponentWrapper extends ReachComponentLike with StackableWrapper2
{
	// ABSTRACT	--------------------------------
	
	override protected def wrapped: ReachComponentLike
	
	
	// IMPLEMENTED	----------------------------
	
	override def parentHierarchy = wrapped.parentHierarchy
	
	override def transparent = wrapped.transparent
	
	override def positionPointer = wrapped.positionPointer
	override def sizePointer = wrapped.sizePointer
	override def boundsPointer = wrapped.boundsPointer
	
	override def paintContent(drawer: Drawer3, drawLevel: DrawLevel, clipZone: Option[Bounds]) =
		wrapped.paintContent(drawer, drawLevel, clipZone)
	
	override def bounds = wrapped.bounds
	override def bounds_=(b: Bounds) = wrapped.bounds = b
	
	override def children = wrapped.children
	
	override def toImage = wrapped.toImage
	
	override def paintMovement(movement: => Vector2D) = wrapped.paintMovement(movement)
	override def paintWith(drawer: Drawer3, clipZone: Option[Bounds]) = wrapped.paintWith(drawer, clipZone)
	override def regionToImage(region: Bounds) = wrapped.regionToImage(region)
}
