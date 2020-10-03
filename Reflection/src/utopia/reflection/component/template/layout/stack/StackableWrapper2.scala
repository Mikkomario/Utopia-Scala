package utopia.reflection.component.template.layout.stack

import utopia.reflection.component.template.ComponentWrapper2

/**
  * This wrapper wraps a stackable, providing full stackable interface itself
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait StackableWrapper2 extends ComponentWrapper2 with Stackable2
{
	// ABSTRACT	---------------------
	
	override protected def wrapped: Stackable2
	
	
	// IMPLEMENTED	-----------------
	
	override def children = wrapped.children
	
	override def updateLayout() = wrapped.updateLayout()
	
	override def stackSize = wrapped.stackSize
	
	override def resetCachedSize() = wrapped.resetCachedSize()
	
	override def stackId = wrapped.stackId
}
