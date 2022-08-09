package utopia.reach.component.template

import utopia.paradigm.color.Color
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.mutable.MutableCustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reflection.shape.stack.StackSize

object MutableCustomDrawReachComponent
{
	// OTHER	---------------------------
	
	/**
	  * Creates a new simplistic custom draw component
	  * @param parentHierarchy This component's parent hierarchy
	  * @param customDrawers Custom drawers initially assigned to this component (default = empty)
	  * @param stackSize Stack size for this component (call by name)
	  * @return A new component
	  */
	def apply(parentHierarchy: ComponentHierarchy, customDrawers: Vector[CustomDrawer] = Vector())
			 (stackSize: => StackSize): MutableCustomDrawReachComponent =
	{
		val c = new BasicComponent(parentHierarchy, stackSize)
		if (customDrawers.nonEmpty)
			c.customDrawers = customDrawers
		c
	}
	
	
	// NESTED	---------------------------
	
	private class BasicComponent(override val parentHierarchy: ComponentHierarchy, getSize: => StackSize)
		extends MutableCustomDrawReachComponent
	{
		// IMPLEMENTED	-------------------
		
		override def calculatedStackSize = getSize
		
		override def updateLayout() = ()
	}
}

/**
  * A common trait for reach component <b>implementations</b> (not wrappers) which implement mutable custom drawing
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
trait MutableCustomDrawReachComponent extends CustomDrawReachComponent with MutableCustomDrawable
{
	// ATTRIBUTES	--------------------------
	
	var customDrawers = Vector[CustomDrawer]()
	
	
	// OTHER	------------------------------
	
	/**
	  * Adds background drawing to this component
	  * @param color Color used when drawing component background
	  */
	def addBackground(color: Color) = addCustomDrawer(BackgroundDrawer(color))
}
