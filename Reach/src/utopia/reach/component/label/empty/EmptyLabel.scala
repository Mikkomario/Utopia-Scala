package utopia.reach.component.label.empty

import utopia.paradigm.color.Color
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade}
import utopia.reflection.component.context.ColorContextLike
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.shape.stack.StackSize

object EmptyLabel
	extends ContextInsertableComponentFactoryFactory[ColorContextLike, EmptyLabelFactory, ContextualEmptyLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new EmptyLabelFactory(hierarchy)
}

class EmptyLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualEmptyLabelFactory]
{
	// IMPLEMENTED  --------------------------------
	
	override def withContext[N <: ColorContextLike](context: N) =
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

case class ContextualEmptyLabelFactory[+N <: ColorContextLike](factory: EmptyLabelFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualEmptyLabelFactory]
{
	// COMPUTED ------------------------------------
	
	/**
	 * @return A copy of this factory without context information
	 */
	def withoutContext = factory
	
	
	// IMPLEMENTED  --------------------------------
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
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
	def withBackgroundForRole(role: ColorRole, stackSize: StackSize, preferredShade: ColorShade = Standard,
	                          customDrawers: Vector[CustomDrawer] = Vector()) =
		factory.withBackground(context.color(role, preferredShade), stackSize, customDrawers)
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
