package utopia.reflection.component.swing

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.stack.StackLeaf
import utopia.reflection.shape.{StackLength, StackSize}

object StackSpace
{
	/**
	  * Creates a space with length specifications towards only one direction (any length is used for the other direction)
	  * @param axis Axis that determines the direction of this spacing
	  * @param length Length of this spacing
	  * @return A new spacing component
	  */
	def along(axis: Axis2D, length: StackLength) = new StackSpace(StackSize(length, StackLength.any, axis))
	
	/**
	  * Creates a space with only width component
	  * @param width Width of this spacing
	  * @return A new spacing with specified width and any height
	  */
	def horizontal(width: StackLength) = along(X, width)
	
	/**
	  * Creates a space with only height component
	  * @param height Height of this spacing
	  * @return A new spacing with specified height and any width
	  */
	def vertical(height: StackLength) = along(Y, height)
	
	/**
	  * Creates a stack space that delegates drawing to specified custom drawer
	  * @param drawer A drawer that will draw the contents of this component
	  * @param size Target size for this component
	  * @return A new stack space component with custom drawing
	  */
	def drawingWith(drawer: CustomDrawer, size: StackSize) =
	{
		val c = new StackSpace(size)
		c.addCustomDrawer(drawer)
		c
	}
}

/**
  * This component is used for spacing within a stackable context. By default this component doesn't draw anything, but
  * it supports custom drawing in case some drawing is required
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class StackSpace(override val stackSize: StackSize) extends JWrapper with StackLeaf with CustomDrawableWrapper
{
	// ATTRIBUTES	---------------------------
	
	val component = new EmptyJComponent
	
	override val stackId = hashCode()
	
	
	// IMPLEMENTED	---------------------------
	
	override def toString = s"StackSpace($stackSize)"
	
	override def updateLayout() = Unit
	
	override def resetCachedSize() = Unit
	
	override def drawable = component
}
