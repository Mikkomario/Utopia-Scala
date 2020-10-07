package utopia.reflection.component.reach.template

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.{CustomDrawable2, CustomDrawer, DrawLevel}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
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
		override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
		
		override def updateLayout() = ()
		
		override def calculatedStackSize = getStackSize
	}
}

/**
  * A reach component implementation that applies custom drawers
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
trait CustomDrawReachComponent extends ReachComponent with CustomDrawable2
{
	// ABSTRACT	-------------------------
	
	protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]): Unit
	
	
	// IMPLEMENTED	---------------------
	
	override def drawBounds = bounds
	
	override protected def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) = {
		val drawers = customDrawers.filter {_.drawLevel == drawLevel}
		// On normal draw level, draws the content
		if (drawLevel == Normal)
			drawContent(drawer, clipZone)
		// Draws with custom drawers
		if (drawers.nonEmpty) {
			val targetBounds = drawBounds
			val d = clipZone.map(drawer.clippedTo).getOrElse(drawer)
			drawers.foreach {_.draw(d, targetBounds)}
		}
	}
}
