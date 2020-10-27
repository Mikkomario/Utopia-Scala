package utopia.reflection.component.reach.template

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.mutable.MutableCustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.shape.stack.StackSize

object MutableCustomDrawReachComponent
{
	// OTHER	---------------------------
	
	/**
	  * Creates a new simplistic custom draw component
	  * @param parentHierarchy This component's parent hierarchy
	  * @param stackSize Stack size for this component (call by name)
	  * @return A new component
	  */
	def apply(parentHierarchy: ComponentHierarchy)(stackSize: => StackSize): MutableCustomDrawReachComponent =
		new BasicComponent(parentHierarchy, stackSize)
	
	
	// NESTED	---------------------------
	
	private class BasicComponent(override val parentHierarchy: ComponentHierarchy, getSize: => StackSize)
		extends MutableCustomDrawReachComponent
	{
		override def calculatedStackSize = getSize
		
		override def updateLayout() = ()
	}
}

/**
  * A common trait for reach component <b>implementations</b> (not wrappers) which implement mutable custom drawing
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
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
