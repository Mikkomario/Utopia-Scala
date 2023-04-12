package utopia.reflection.component.swing.template

import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackLeaf}
import utopia.reflection.shape.stack.StackSize

import java.awt.Component

object StackableAwtComponentWrapper
{
    /**
     * Wraps a component as stackable
     * @param component wrapped component
     * @param getSize a function for retrieving component size
     */
    def apply(component: Component, getSize: () => StackSize, update: () => Unit = () => ()): StackableAwtComponentWrapper =
		new StackWrapper(component, getSize, update)
    
    /**
     * Wraps a component as stackable
     * @param component wrapped component
     * @param size fixed component sizes
     */
    def apply(component: Component, size: StackSize): StackableAwtComponentWrapper = apply(component, () => size)
}

/**
* This trait is inherited by component classes that can be placed in stacks
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait StackableAwtComponentWrapper extends AwtComponentWrapper with CachingReflectionStackable
{
	// IMPLEMENTED	-----------------
	
	override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapper].visible_=(visible)
	
	override def visible_=(isVisible: Boolean) = super[CachingReflectionStackable].visible_=(isVisible)
	
	
	// OTHER	---------------------
	
	/*
	  * @param margins The margins placed around this instance
	  * @return A framing that contains this stackable item
	  */
	// def framed(margins: StackSize) = new Framing[StackableAwtComponentWrapper](this, margins)
}

private class StackWrapper(val component: Component, val getSize: () => StackSize, val update: () => Unit)
	extends StackableAwtComponentWrapper with ReflectionStackLeaf
{
	override def updateLayout() = update()
	
	override def calculatedStackSize = getSize()
}