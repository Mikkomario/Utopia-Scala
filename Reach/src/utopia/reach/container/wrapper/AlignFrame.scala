package utopia.reach.container.wrapper

import utopia.firmament.component.container.single.AlignFrameLike
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.collection.immutable.Empty
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, ReachComponent}
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}

object AlignFrame extends Cff[AlignFrameFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new AlignFrameFactory(hierarchy)
}

trait AlignFrameFactoryLike[+Repr]
	extends WrapperContainerFactory[AlignFrame, ReachComponent] with CustomDrawableFactory[Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The alignment used in this factory
	  */
	def alignment: Alignment
	/**
	  * @return Whether the component should be scaled to fill the non-aligned sides.
	  */
	def scalesToFill: Boolean
	
	/**
	  * @return Copy of this factory which scales the component to fill the non-aligned sides.
	  *         E.g. When aligning to top, this would
	  *         scale the component to fill the horizontal space of this container.
	  */
	def scalingToFillSides: Repr
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply[C <: ReachComponent, R](content: OpenComponent[C, R]): ComponentWrapResult[AlignFrame, C, R] = {
		val frame = new AlignFrame(hierarchy, content.component, alignment, customDrawers, scalesToFill)
		content attachTo frame
	}
}

class AlignFrameFactory(val parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[Any, ContextualAlignFrameFactory]
		with FromAlignmentFactory[InitializedAlignFrameFactory]
{
	// IMPLEMENTED  ------------------------------
	
	/**
	  * @param alignment The alignment to use
	  * @return A factory that uses the specified alignment
	  */
	override def apply(alignment: Alignment) = InitializedAlignFrameFactory(parentHierarchy, alignment)
	
	override def withContext[N <: Any](context: N) =
		ContextualAlignFrameFactory(parentHierarchy, context)
	
	
	// OTHER    ----------------------------------
	
	/**
	 * Creates a new align frame
	 * @param content Content to place in this frame (open)
	 * @param alignment Alignment to use
	 * @param customDrawers Custom drawers to assign (default = empty)
	 * @tparam C Type of wrapped component
	 * @tparam R Type of additional creation result
	 * @return A new align frame
	 */
	@deprecated("Please use .apply(Alignment).apply(OpenComponent) instead", "v1.1")
	def apply[C <: ReachComponent, R](content: OpenComponent[C, R], alignment: Alignment,
	                                  customDrawers: Seq[CustomDrawer] = Empty) =
	{
		val frame = new AlignFrame(parentHierarchy, content.component, alignment, customDrawers)
		content attachTo frame
	}
}

case class InitializedAlignFrameFactory(hierarchy: ComponentHierarchy, alignment: Alignment,
                                        customDrawers: Seq[CustomDrawer] = Empty, scalesToFill: Boolean = false)
	extends AlignFrameFactoryLike[InitializedAlignFrameFactory]
		with NonContextualWrapperContainerFactory[AlignFrame, ReachComponent]
		with FromGenericContextFactory[Any, InitializedContextualAlignFrameFactory]
{
	override def scalingToFillSides: InitializedAlignFrameFactory = copy(scalesToFill = true)
	
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): InitializedAlignFrameFactory =
		copy(customDrawers = drawers)
	
	override def withContext[N <: Any](context: N): InitializedContextualAlignFrameFactory[N] =
		InitializedContextualAlignFrameFactory(hierarchy, context, alignment, customDrawers)
}

case class ContextualAlignFrameFactory[N](parentHierarchy: ComponentHierarchy, context: N)
	extends GenericContextualFactory[N, Any, ContextualAlignFrameFactory]
		with FromAlignmentFactory[InitializedContextualAlignFrameFactory[N]]
{
	// COMPUTED ------------------------------
	
	/**
	 * @return A copy of this factory with no contextual information
	 */
	@deprecated("Deprecated for removal", "v1.1")
	def withoutContext = new AlignFrameFactory(parentHierarchy)
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(alignment: Alignment) =
		InitializedContextualAlignFrameFactory(parentHierarchy, context, alignment)
	
	override def withContext[N2 <: Any](newContext: N2) = copy(context = newContext)
}

case class InitializedContextualAlignFrameFactory[N](hierarchy: ComponentHierarchy, context: N,
                                                     alignment: Alignment, customDrawers: Seq[CustomDrawer] = Empty,
                                                     scalesToFill: Boolean = false)
	extends AlignFrameFactoryLike[InitializedContextualAlignFrameFactory[N]]
		with ContextualWrapperContainerFactory[N, Any, AlignFrame, ReachComponent, InitializedContextualAlignFrameFactory]
{
	override def scalingToFillSides: InitializedContextualAlignFrameFactory[N] = copy(scalesToFill = true)
	
	override def withContext[N2 <: Any](newContext: N2): InitializedContextualAlignFrameFactory[N2] =
		copy(context = newContext)
	
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): InitializedContextualAlignFrameFactory[N] =
		copy(customDrawers = drawers)
}

/**
 * A container which contains a single item, aligned to some side
 * @author Mikko Hilpinen
 * @since 30.1.2021, v0.1
 */
class AlignFrame(override val hierarchy: ComponentHierarchy, override val content: ReachComponent,
                 override val alignment: Alignment, override val customDrawers: Seq[CustomDrawer] = Empty,
                 override val scaleToFill: Boolean = false)
	extends ConcreteCustomDrawReachComponent with AlignFrameLike[ReachComponent]