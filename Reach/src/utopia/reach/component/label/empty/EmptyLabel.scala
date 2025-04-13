package utopia.reach.component.label.empty

import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.factory.contextual.{ColorContextualFactory, ContextualBackgroundAssignableFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy}

case class EmptyLabelFactory(hierarchy: ComponentHierarchy, customDrawers: Seq[CustomDrawer] = Empty)
	extends FromContextFactory[StaticColorContext, ContextualEmptyLabelFactory] with PartOfComponentHierarchy
		with CustomDrawableFactory[EmptyLabelFactory]
{
	// IMPLEMENTED  --------------------------------
	
	override def withContext(context: StaticColorContext) =
		ContextualEmptyLabelFactory(hierarchy, context, customDrawers)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): EmptyLabelFactory = copy(customDrawers = drawers)
	
	
	// OTHER    ------------------------------------
	
	/**
	  * Creates a new empty label
	  * @param stackSize Stack size of this label
	  * @return A new empty label
	  */
	def apply(stackSize: StackSize) = new EmptyLabel(hierarchy, stackSize, customDrawers)
	/**
	  * Creates a new empty label
	  * @param stackSize     Stack size of this label
	  * @param customDrawers Custom drawers to assign to this label
	  * @return A new empty label
	  */
	@deprecated("Deprecated for removal. Please use .withCustomDrawers(...).apply(StackSize) instead", "v1.6")
	def apply(stackSize: StackSize, customDrawers: Seq[CustomDrawer]): EmptyLabel =
		withAdditionalCustomDrawers(customDrawers).apply(stackSize)
	
	/**
	  * @param color Background color to assign
	  * @return Copy of this factory which draws the specified background color
	  */
	def withBackground(color: Color) = withCustomBackgroundDrawer(BackgroundDrawer(color))
	/**
	  * @param colorPointer A background color pointer
	  * @return Copy of this factory which draws a background based on the specified pointer's value
	  */
	def withBackground(colorPointer: Changing[Color]) =
		withCustomBackgroundDrawer(BackgroundViewDrawer(colorPointer))
	/**
	 * Creates a new empty label with a static background color
	 * @param color Background color for this label
	 * @param stackSize Stack size of this label
	 * @param customDrawers Additional custom drawers to assign (default = empty)
	 * @return A new empty label
	 */
	@deprecated("Deprecated for removal. Please use .withBackground(Color).apply(StackSize) instead", "v1.6")
	def withBackground(color: Color, stackSize: StackSize, customDrawers: Seq[CustomDrawer] = Empty) =
		apply(stackSize, BackgroundDrawer(color) +: customDrawers)
}

case class ContextualEmptyLabelFactory(hierarchy: ComponentHierarchy, context: StaticColorContext,
                                       customDrawers: Seq[CustomDrawer] = Empty)
	extends ColorContextualFactory[ContextualEmptyLabelFactory]
		with ContextualBackgroundAssignableFactory[StaticColorContext, ContextualEmptyLabelFactory]
		with CustomDrawableFactory[ContextualEmptyLabelFactory]
{
	// COMPUTED ------------------------------------
	
	/**
	 * @return A copy of this factory without context information
	 */
	@deprecated("Deprecated for removal", "v1.1")
	def withoutContext = EmptyLabelFactory(hierarchy, customDrawers)
	
	
	// IMPLEMENTED  --------------------------------
	
	override def self: ContextualEmptyLabelFactory = this
	
	override def withContext(newContext: StaticColorContext) = copy(context = newContext)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualEmptyLabelFactory =
		copy(customDrawers = drawers)
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param size Size assigned for the created label
	  * @return A new empty label
	  */
	def apply(size: StackSize) = new EmptyLabel(hierarchy, size, customDrawers)
	
	/**
	 * Creates a new empty label which is filled with a background color suitable for a specific role
	 * @param role Color role of this label
	 * @param stackSize Stack size of this label
	 * @param preferredShade Preferred color shade (default = standard)
	 * @param customDrawers Additional custom drawers to assign (default = empty)
	 * @return A new empty label
	 */
	@deprecated("Please use .withBackground(ColorRole, ColorLevel).apply(StackSize) instead", "v1.1")
	def withBackgroundForRole(role: ColorRole, stackSize: StackSize, preferredShade: ColorLevel = Standard,
	                          customDrawers: Seq[CustomDrawer] = Empty) =
		withoutContext.withBackground(context.color.preferring(preferredShade)(role), stackSize, customDrawers)
}

object EmptyLabel extends Cff[EmptyLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = EmptyLabelFactory(hierarchy)
}
/**
 * A simple immutable empty label
 * @author Mikko Hilpinen
 * @since 29.1.2021, v0.1
 */
class EmptyLabel(override val hierarchy: ComponentHierarchy, override val calculatedStackSize: StackSize,
                 override val customDrawers: Seq[CustomDrawer])
	extends ConcreteCustomDrawReachComponent
{
	override def updateLayout() = ()
}
