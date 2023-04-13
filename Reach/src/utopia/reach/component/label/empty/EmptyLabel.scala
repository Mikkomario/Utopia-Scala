package utopia.reach.component.label.empty

import utopia.firmament.context.ColorContext
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.paradigm.color.ColorLevel.Standard
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.firmament.model.stack.StackSize

object EmptyLabel
	extends ContextInsertableComponentFactoryFactory[ColorContext, EmptyLabelFactory, ContextualEmptyLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new EmptyLabelFactory(hierarchy)
}

class EmptyLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContext, ContextualEmptyLabelFactory]
{
	// IMPLEMENTED  --------------------------------
	
	override def withContext[N <: ColorContext](context: N) =
		ContextualEmptyLabelFactory(this, context)
	
	
	// OTHER    ------------------------------------
	
	/**
	 * Creates a new empty label
	 * @param stackSize Stack size of this label
	 * @param customDrawers Custom drawers to assign to this label (default = empty)
	 * @return A new empty label
	 */
	def apply(stackSize: StackSize, customDrawers: Vector[CustomDrawer] = Vector()) =
		new EmptyLabel(parentHierarchy, stackSize, customDrawers)
	
	/**
	 * Creates a new empty label with a static background color
	 * @param color Background color for this label
	 * @param stackSize Stack size of this label
	 * @param customDrawers Additional custom drawers to assign (default = empty)
	 * @return A new empty label
	 */
	def withBackground(color: Color, stackSize: StackSize, customDrawers: Vector[CustomDrawer] = Vector()) =
		apply(stackSize, BackgroundDrawer(color) +: customDrawers)
}

case class ContextualEmptyLabelFactory[+N <: ColorContext](factory: EmptyLabelFactory, context: N)
	extends ContextualComponentFactory[N, ColorContext, ContextualEmptyLabelFactory]
{
	// COMPUTED ------------------------------------
	
	/**
	 * @return A copy of this factory without context information
	 */
	def withoutContext = factory
	
	
	// IMPLEMENTED  --------------------------------
	
	override def withContext[N2 <: ColorContext](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER    ------------------------------------
	
	/**
	 * Creates a new empty label which is filled with a background color suitable for a specific role
	 * @param role Color role of this label
	 * @param stackSize Stack size of this label
	 * @param preferredShade Preferred color shade (default = standard)
	 * @param customDrawers Additional custom drawers to assign (default = empty)
	 * @return A new empty label
	 */
	def withBackgroundForRole(role: ColorRole, stackSize: StackSize, preferredShade: ColorLevel = Standard,
	                          customDrawers: Vector[CustomDrawer] = Vector()) =
		factory.withBackground(context.color.preferring(preferredShade)(role), stackSize, customDrawers)
}

/**
 * A simple immutable empty label
 * @author Mikko Hilpinen
 * @since 29.1.2021, v0.1
 */
class EmptyLabel(override val parentHierarchy: ComponentHierarchy, override val calculatedStackSize: StackSize,
                 override val customDrawers: Vector[CustomDrawer])
	extends CustomDrawReachComponent
{
	override def updateLayout() = ()
}
