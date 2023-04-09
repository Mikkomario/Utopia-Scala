package utopia.reach.component.template

import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.graphics.Drawer
import utopia.paradigm.shape.shape2d.Bounds
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reflection.component.drawing.template.{CustomDrawable, CustomDrawer, DrawLevel}
import utopia.reflection.shape.stack.StackSize

object CustomDrawReachComponent
{
	// OTHER	------------------------------
	
	/**
	  * Creates a new simplistic custom draw component
	  * @param parentHierarchy This component's parent hierarchy
	  * @param customDrawers Drawers used with this component
	  * @param stackSize Stack size to be used with this component (call by name)
	  * @return A new custom draw component
	  */
	def apply(parentHierarchy: ComponentHierarchy, customDrawers: Vector[CustomDrawer])
			 (stackSize: => StackSize): CustomDrawReachComponent =
		new BasicComponent(parentHierarchy, customDrawers, stackSize)
	
	
	// NESTED	------------------------------
	
	private class BasicComponent(override val parentHierarchy: ComponentHierarchy,
								 override val customDrawers: Vector[CustomDrawer],
								 getStackSize: => StackSize)
		extends CustomDrawReachComponent
	{
		override def updateLayout() = ()
		
		override def calculatedStackSize = getStackSize
	}
}

/**
  * A reach component implementation that applies custom drawers
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
trait CustomDrawReachComponent extends ReachComponent with CustomDrawable
{
	// IMPLEMENTED	--------------------------
	
	override def drawBounds = bounds
	
	override def transparent = customDrawers.forall { _.transparent }
	
	override def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) =
	{
		val drawers = customDrawers.filter { _.drawLevel == drawLevel }
		// Draws with custom drawers
		if (drawers.nonEmpty) {
			val targetBounds = drawBounds
			val d = clipZone.map(drawer.clippedToBounds).getOrElse(drawer)
			drawers.view.takeTo { _.opaque }.foreach { _.draw(d, targetBounds) }
		}
	}
}
