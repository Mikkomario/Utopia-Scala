package utopia.reach.component.template

import utopia.firmament.drawing.template.{CustomDrawable, CustomDrawer}
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.reach.component.hierarchy.ComponentHierarchy

object ConcreteCustomDrawReachComponent
{
	// OTHER	------------------------------
	
	/**
	  * Creates a new simplistic custom draw component
	  * @param hierarchy This component's parent hierarchy
	  * @param customDrawers Drawers used with this component
	  * @param stackSize Stack size to be used with this component (call by name)
	  * @return A new custom draw component
	  */
	def apply(hierarchy: ComponentHierarchy, customDrawers: Seq[CustomDrawer])
			 (stackSize: => StackSize): ConcreteCustomDrawReachComponent =
		new BasicComponent(hierarchy, customDrawers, stackSize)
	
	
	// NESTED	------------------------------
	
	private class BasicComponent(override val hierarchy: ComponentHierarchy,
	                             override val customDrawers: Seq[CustomDrawer],
	                             getStackSize: => StackSize)
		extends ConcreteCustomDrawReachComponent
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
trait ConcreteCustomDrawReachComponent extends ConcreteReachComponent with CustomDrawable
{
	// IMPLEMENTED	--------------------------
	
	override def drawBounds = bounds
	
	override def transparent = customDrawers.forall { _.transparent }
	
	override def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) = {
		val drawers = customDrawers.filter { _.drawLevel == drawLevel }
		// Draws with custom drawers
		if (drawers.nonEmpty) {
			val targetBounds = drawBounds
			val d = clipZone.map(drawer.clippedToBounds).getOrElse(drawer)
			drawers.view.takeTo { _.opaque }.foreach { _.draw(d, targetBounds) }
		}
	}
}
