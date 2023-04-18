package utopia.reach.component.label.empty

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.{ColorContextualFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent

object ViewEmptyLabel extends Cff[ViewEmptyLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewEmptyLabelFactory(hierarchy)
}

class ViewEmptyLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[ColorContext, ContextualViewEmptyLabelFactory]
{
	// IMPLEMENTED  ------------------------------
	
	override def withContext(context: ColorContext) = ContextualViewEmptyLabelFactory(this, context)
	
	
	// OTHER    ----------------------------------
	
	/**
	 * Creates a new label
	 * @param stackSizePointer Pointer to this label's stack size
	 * @param customDrawersPointer Pointer to this label's custom drawers (default = always empty)
	 * @return A new label
	 */
	def apply(stackSizePointer: Changing[StackSize],
	          customDrawersPointer: Changing[Vector[CustomDrawer]] = Fixed(Vector())) =
		new ViewEmptyLabel(parentHierarchy, stackSizePointer, customDrawersPointer)
	
	/**
	 * Creates a new label with fixed stack size
	 * @param stackSize This label's stack size
	 * @param customDrawersPointer Pointer to this label's custom drawers
	 * @return A new label
	 */
	def withStaticSize(stackSize: StackSize, customDrawersPointer: Changing[Vector[CustomDrawer]]) =
		apply(Fixed(stackSize), customDrawersPointer)
	
	/**
	 * Creates a new label with fixed custom drawers
	 * @param stackSizePointer Pointer to this label's stack size
	 * @param customDrawers Custom drawers to assign to this label
	 * @return A new label
	 */
	def withStaticDrawers(stackSizePointer: Changing[StackSize], customDrawers: Vector[CustomDrawer]) =
		apply(stackSizePointer, Fixed(customDrawers))
	
	/**
	 * Creates a new label with a changing background color
	 * @param backgroundPointer A pointer to this label's background color
	 * @param stackSizePointer Pointer to this label's stack size
	 * @return A new label
	 */
	def withBackground(backgroundPointer: Changing[Color], stackSizePointer: Changing[StackSize]) =
	{
		val label = withStaticDrawers(stackSizePointer, Vector(BackgroundViewDrawer(backgroundPointer)))
		backgroundPointer.addContinuousAnyChangeListener { label.repaint() }
		label
	}
}

case class ContextualViewEmptyLabelFactory(factory: ViewEmptyLabelFactory, context: ColorContext)
	extends ColorContextualFactory[ContextualViewEmptyLabelFactory]
{
	// COMPUTED -----------------------------------
	
	/**
	 * @return A copy of this factory without contextual information
	 */
	def withoutContext = factory
	
	
	// IMPLEMENTED  -------------------------------
	
	override def self: ContextualViewEmptyLabelFactory = this
	
	override def withContext(newContext: ColorContext) = copy(context = newContext)
	
	
	// OTHER    -----------------------------------
	
	/**
	 * Creates a new label with changing background color
	 * @param rolePointer Pointer to background color role
	 * @param stackSizePointer Pointer to this label's stack size
	 * @param preferredShade Preferred color shade to use
	 * @return A new label
	 */
	def withBackgroundForRole(rolePointer: Changing[ColorRole], stackSizePointer: Changing[StackSize],
	                          preferredShade: ColorLevel = Standard) =
		factory.withBackground(rolePointer.map { context.color.preferring(preferredShade)(_) }, stackSizePointer)
}

/**
 * A pointer-based empty label
 * @author Mikko Hilpinen
 * @since 29.1.2021, v0.1
 */
class ViewEmptyLabel(override val parentHierarchy: ComponentHierarchy, val stackSizePointer: Changing[StackSize],
                     val customDrawersPointer: Changing[Vector[CustomDrawer]]) extends CustomDrawReachComponent
{
	// INITIAL CODE -------------------------------
	
	// Reacts to pointer changes
	stackSizePointer.addContinuousAnyChangeListener { revalidate() }
	customDrawersPointer.addContinuousAnyChangeListener { repaint() }
	
	
	// IMPLEMENTED  -------------------------------
	
	override def calculatedStackSize = stackSizePointer.value
	
	override def updateLayout() = ()
	
	override def customDrawers = customDrawersPointer.value
}
