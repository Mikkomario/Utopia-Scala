package utopia.reach.component.template

import utopia.firmament.drawing.mutable.MutableCustomDrawable
import utopia.paradigm.color.Color
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.immutable.Empty

object MutableConcreteCustomDrawReachComponent
{
	// OTHER	---------------------------
	
	/**
	  * Creates a new simplistic custom draw component
	  * @param hierarchy This component's parent hierarchy
	  * @param customDrawers Custom drawers initially assigned to this component (default = empty)
	  * @param stackSize Stack size for this component (call by name)
	  * @return A new component
	  */
	def apply(hierarchy: ComponentHierarchy, customDrawers: Seq[CustomDrawer] = Empty)
			 (stackSize: => StackSize): MutableConcreteCustomDrawReachComponent =
	{
		val c = new BasicComponent(hierarchy, stackSize)
		if (customDrawers.nonEmpty)
			c.customDrawers = customDrawers
		c
	}
	
	
	// NESTED	---------------------------
	
	private class BasicComponent(override val hierarchy: ComponentHierarchy, getSize: => StackSize)
		extends MutableConcreteCustomDrawReachComponent
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
trait MutableConcreteCustomDrawReachComponent extends ConcreteCustomDrawReachComponent with MutableCustomDrawable
{
	// ATTRIBUTES	--------------------------
	
	var customDrawers: Seq[CustomDrawer] = Empty
	
	
	// OTHER	------------------------------
	
	/**
	  * Adds background drawing to this component
	  * @param color Color used when drawing component background
	  */
	def addBackground(color: Color) = addCustomDrawer(BackgroundDrawer(color))
}
