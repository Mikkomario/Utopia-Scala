package utopia.reach.container.multi

import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.stack.StackLength
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponsePriority.After
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, ReachComponent}

/**
  * Common trait for factories that are used for constructing collections
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
trait ViewCollectionFactoryLike[+Repr]
	extends AnyCollectionFactoryLike[Repr] with ViewContainerFactory[Collection, ReachComponent]
{
	// IMPLEMENTED  -------------------
	
	override protected def _apply(contentPointer: Changing[Seq[ReachComponent]]): Collection =
		new ViewCollection(hierarchy, contentPointer, primaryAxis, insideRowLayout, betweenRowsLayout,
			innerMarginPointer, outerMarginPointer, splitThresholdPointer, customDrawers)
}
/**
  * Factory class used for constructing view-based collections using contextual component creation
  * information
  * @param customInnerMarginPointer A user-specified pointer that determines the margin placed
  *                                 between the items in this collection.
  *                                 None if default margin should be applied instead.
  * @param areRelated               Whether the items in this collection should be considered
  *                                 closely related to each other,
  *                                 resulting in a smaller margin placed between them.
  * @tparam N Type of context used and passed along by this factory
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
case class ContextualViewCollectionFactory[+N <: BaseContextPropsView](hierarchy: ComponentHierarchy, context: N,
                                                                       settings: CollectionSettings,
                                                                       customInnerMarginPointer: Option[Changing[StackLength]] = None,
                                                                       areRelated: Boolean = false)
	extends ViewCollectionFactoryLike[ContextualViewCollectionFactory[N]]
		with AnyContextualCollectionFactory[N, ContextualViewCollectionFactory]
		with ContextualViewContainerFactory[N, BaseContextPropsView, Collection, ReachComponent, ContextualViewCollectionFactory]
{
	// IMPLEMENTED  ----------------------
	
	override def withContext[N2 <: BaseContextPropsView](context: N2) =
		copy(context = context)
	override def withSettings(settings: CollectionSettings) = copy(settings = settings)
	
	def withAreRelated(related: Boolean) = copy(areRelated = related)
	def withInnerMarginPointer(p: Option[Changing[StackLength]]) =
		copy(customInnerMarginPointer = p)
}
/**
  * Factory class that is used for constructing view-based collections without using contextual information
  * @param innerMarginPointer A pointer that specifies the margin placed between the in this
  *                           collection.
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
case class ViewCollectionFactory(hierarchy: ComponentHierarchy,
                                 settings: CollectionSettings = CollectionSettings.default,
                                 innerMarginPointer: Changing[StackLength] = Fixed(StackLength.any))
	extends ViewCollectionFactoryLike[ViewCollectionFactory]
		with FromGenericContextFactory[BaseContextPropsView, ContextualViewCollectionFactory]
		with NonContextualViewContainerFactory[Collection, ReachComponent]
{
	// IMPLEMENTED  ------------------------
	
	override def withContext[N <: BaseContextPropsView](context: N) =
		ContextualViewCollectionFactory(hierarchy, context, settings)
	override def withSettings(settings: CollectionSettings) = copy(settings = settings)
	
	/**
	  * @param p A pointer that specifies the margin placed between the in this collection.
	  * @return Copy of this factory with the specified inner margin pointer
	  */
	def withInnerMarginPointer(p: Changing[StackLength]) = copy(innerMarginPointer = p)
}

/**
  * Used for defining collection creation settings outside the component building process
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
case class ViewCollectionSetup(settings: CollectionSettings = CollectionSettings.default)
	extends AnyCollectionSetup[ViewCollectionFactory, ContextualViewCollectionFactory, ViewCollectionSetup]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = ViewCollectionFactory(hierarchy, settings)
	
	override def withContext[N <: BaseContextPropsView](hierarchy: ComponentHierarchy, context: N) =
		ContextualViewCollectionFactory(hierarchy, context, settings)
	override def withSettings(settings: CollectionSettings) = copy(settings = settings)
}

object ViewCollection extends ViewCollectionSetup()
{
	def apply(settings: CollectionSettings) = withSettings(settings)
}
/**
  * A view-based implementation of the Collection trait
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
private class ViewCollection(override val hierarchy: ComponentHierarchy, contentP: Changing[Seq[ReachComponent]],
                             override val primaryAxis: Axis2D, override val insideRowLayout: StackLayout,
                             override val betweenRowsLayout: StackLayout,
                             override val innerMarginPointer: Changing[StackLength],
                             override val outerMarginPointer: Changing[StackLength],
                             splitThresholdPointer: Changing[Option[Double]],
                             override val customDrawers: Seq[CustomDrawer])
	extends ConcreteCustomDrawReachComponent with Collection
{
	// ATTRIBUTES   ----------------------------
	
	private val revalidateAfterChange = ChangeListener.onAnyChange { revalidate() }
	
	
	// INITIAL CODE ----------------------------
	
	setupMarginListeners()
	splitThresholdPointer.addListenerWhile(linkedFlag, After)(revalidateAfterChange)
	contentP.addListenerWhile(linkedFlag, After)(revalidateAfterChange)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def components: Seq[ReachComponent] = contentP.value
	override def splitThreshold: Option[Double] = splitThresholdPointer.value
}