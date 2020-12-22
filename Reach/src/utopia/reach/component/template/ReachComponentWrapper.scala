package utopia.reach.component.template

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.template.layout.stack.StackableWrapper2

/**
  * A common trait for Reach component wrapper classes
  * @author Mikko Hilpinen
  * @since 24.10.2020, v2
  */
trait ReachComponentWrapper extends ReachComponentLike with StackableWrapper2
{
	// ABSTRACT	--------------------------------
	
	override protected def wrapped: ReachComponentLike
	
	
	// IMPLEMENTED	----------------------------
	
	override def children = wrapped.children
	
	override def positionPointer = wrapped.positionPointer
	
	override def sizePointer = wrapped.sizePointer
	
	override def boundsPointer = wrapped.boundsPointer
	
	override def parentHierarchy = wrapped.parentHierarchy
	
	override def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) =
		wrapped.paintContent(drawer, drawLevel, clipZone)
	
	override def bounds_=(b: Bounds) = wrapped.bounds = b
}
