package utopia.reach.component.label.empty

import utopia.flow.event.{ChangingLike, Fixed}
import utopia.genesis.color.Color
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade}
import utopia.reflection.component.context.ColorContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.BackgroundViewDrawer
import utopia.reflection.shape.stack.StackSize

object ViewEmptyLabel extends ContextInsertableComponentFactoryFactory[ColorContextLike, ViewEmptyLabelFactory,
	ContextualViewEmptyLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewEmptyLabelFactory(hierarchy)
}

class ViewEmptyLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualViewEmptyLabelFactory]
{
	// IMPLEMENTED  ------------------------------
	
	override def withContext[N <: ColorContextLike](context: N) =
		ContextualViewEmptyLabelFactory(this, context)
	
	
	// OTHER    ----------------------------------
	
	/**
	 * Creates a new label
	 * @param stackSizePointer Pointer to this label's stack size
	 * @param customDrawersPointer Pointer to this label's custom drawers (default = always empty)
	 * @return A new label
	 */
	def apply(stackSizePointer: ChangingLike[StackSize],
	          customDrawersPointer: ChangingLike[Vector[CustomDrawer]] = Fixed(Vector())) =
		new ViewEmptyLabel(parentHierarchy, stackSizePointer, customDrawersPointer)
	
	/**
	 * Creates a new label with fixed stack size
	 * @param stackSize This label's stack size
	 * @param customDrawersPointer Pointer to this label's custom drawers
	 * @return A new label
	 */
	def withStaticSize(stackSize: StackSize, customDrawersPointer: ChangingLike[Vector[CustomDrawer]]) =
		apply(Fixed(stackSize), customDrawersPointer)
	
	/**
	 * Creates a new label with fixed custom drawers
	 * @param stackSizePointer Pointer to this label's stack size
	 * @param customDrawers Custom drawers to assign to this label
	 * @return A new label
	 */
	def withStaticDrawers(stackSizePointer: ChangingLike[StackSize], customDrawers: Vector[CustomDrawer]) =
		apply(stackSizePointer, Fixed(customDrawers))
	
	/**
	 * Creates a new label with a changing background color
	 * @param backgroundPointer A pointer to this label's background color
	 * @param stackSizePointer Pointer to this label's stack size
	 * @return A new label
	 */
	def withBackground(backgroundPointer: ChangingLike[Color], stackSizePointer: ChangingLike[StackSize]) =
	{
		val label = withStaticDrawers(stackSizePointer, Vector(BackgroundViewDrawer(backgroundPointer)))
		backgroundPointer.addAnyChangeListener { label.repaint() }
		label
	}
}

case class ContextualViewEmptyLabelFactory[+N <: ColorContextLike](factory: ViewEmptyLabelFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualViewEmptyLabelFactory]
{
	// COMPUTED -----------------------------------
	
	/**
	 * @return A copy of this factory without contextual information
	 */
	def withoutContext = factory
	
	
	// IMPLEMENTED  -------------------------------
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER    -----------------------------------
	
	/**
	 * Creates a new label with changing background color
	 * @param rolePointer Pointer to background color role
	 * @param stackSizePointer Pointer to this label's stack size
	 * @param preferredShade Preferred color shade to use
	 * @return A new label
	 */
	def withBackgroundForRole(rolePointer: ChangingLike[ColorRole], stackSizePointer: ChangingLike[StackSize],
	                          preferredShade: ColorShade = Standard) =
		factory.withBackground(rolePointer.map { context.color(_, preferredShade) }, stackSizePointer)
}

/**
 * A pointer-based empty label
 * @author Mikko Hilpinen
 * @since 29.1.2021, v0.1
 */
class ViewEmptyLabel(override val parentHierarchy: ComponentHierarchy, val stackSizePointer: ChangingLike[StackSize],
                     val customDrawersPointer: ChangingLike[Vector[CustomDrawer]]) extends CustomDrawReachComponent
{
	// INITIAL CODE -------------------------------
	
	// Reacts to pointer changes
	stackSizePointer.addAnyChangeListener { revalidate() }
	customDrawersPointer.addAnyChangeListener { repaint() }
	
	
	// IMPLEMENTED  -------------------------------
	
	override def calculatedStackSize = stackSizePointer.value
	
	override def updateLayout() = ()
	
	override def customDrawers = customDrawersPointer.value
}
