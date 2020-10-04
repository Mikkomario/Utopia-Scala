package utopia.reflection.component.reach.template

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
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
		override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
		
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
}
