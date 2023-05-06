package utopia.reach.container.multi

import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout.{Center, Fit}
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.DetachmentChoice
import utopia.flow.util.NotEmpty
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.FlagLike.wrap
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.hierarchy.{ComponentHierarchy, SeedHierarchyBlock}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentWrapResult.SwitchableComponentsWrapResult
import utopia.reach.component.wrapper.OpenComponent.SwitchableOpenComponents
import utopia.reach.component.wrapper.{ComponentWrapResult, Open, OpenComponent}

object ViewStack extends Cff[ViewStackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewStackFactory(hierarchy)
}

trait ViewStackFactoryLike[+Repr]
	extends ViewContainerFactory[Stack, ReachComponentLike] with CustomDrawableFactory[Repr]
{
	// ABSTRACT ---------------------------
	
	protected def axisPointer: Changing[Axis2D]
	protected def layoutPointer: Changing[StackLayout]
	protected def marginPointer: Changing[StackLength]
	protected def capPointer: Changing[StackLength]
	
	protected def segmentGroup: Option[SegmentGroup]
	
	/**
	  * @param p Pointer that contains the axis to use on this stack
	  * @return A copy of this factory that uses the specified pointer
	  */
	def withAxisPointer(p: Changing[Axis2D]): Repr
	/**
	  * @param p Pointer that contains the layout to use on this stack
	  * @return A copy of this factory that uses the specified pointer
	  */
	def withLayoutPointer(p: Changing[StackLayout]): Repr
	/**
	  * @param p Pointer that contains the margin to place between items inside this stack
	  * @return A copy of this factory that uses the specified pointer
	  */
	def withMarginPointer(p: Changing[StackLength]): Repr
	/**
	  * @param p Pointer that contains the margin to place at each end of this stack
	  * @return A copy of this factory that uses the specified pointer
	  */
	def withCapPointer(p: Changing[StackLength]): Repr
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Copy of this factory that builds rows
	  */
	def row = withAxis(X)
	
	/**
	  * @return Copy of this factory where items are centered
	  */
	def centered = withLayout(Center)
	
	/**
	  * @return Copy of this factory that doesn't allow for any stack margins
	  */
	def withoutMargin = withMargin(StackLength.fixedZero)
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply[C <: ReachComponentLike, R](content: SwitchableOpenComponents[C, R]): SwitchableComponentsWrapResult[Stack, C, R] =
	{
		// Creates either a static stack or a view stack, based on whether the pointers are actually used
		// Case: All parameters are fixed values => Creates an immutable stack
		if (content.isEmpty || (axisPointer.isFixed &&
			layoutPointer.isFixed && marginPointer.isFixed && capPointer.isFixed &&
			content.forall { _.result.isFixed }))
		{
			// Removes content that will never be visible
			val remainingContent = content.filter { _.result.value }
			val stackF = Stack(parentHierarchy)
				.copy(axis = axisPointer.value, layout = layoutPointer.value,
					margin = marginPointer.value, cap = capPointer.value, customDrawers = customDrawers)
			// Uses segmentation if available
			val stack = segmentGroup match {
				// Case: Segmentation used
				case Some(group) => stackF.withSegments(content, group)
				// Case: No segmentation used
				case None =>
					// Merges the content under a single OpenComponent & ComponentHierarchy instance
					val mergedContent = NotEmpty(remainingContent) match {
						case Some(content) => new OpenComponent(content.map { _.component }, content.head.hierarchy)
						case None => new OpenComponent(Vector[C](), new SeedHierarchyBlock(parentHierarchy.top))
					}
					stackF(mergedContent)
			}
			stack.mapChild { _.map { _ -> AlwaysTrue } }.withResult(content.result)
		}
		// Case: Values include changing values => Creates a view stack
		else {
			// May use segmentation
			segmentGroup match {
				// Case: Segmentation used
				case Some(group) =>
					// WET WET
					// Wraps the components into segments before placing them in this stack
					val wrappers = Open.many { hierarchies =>
						group.wrap(content) { hierarchies.next() }.map { _.parentAndResult }
					}.component
					val stack = new ViewStack(parentHierarchy, wrappers.map { _.componentAndResult },
						axisPointer, layoutPointer, marginPointer, capPointer, customDrawers)
					wrappers.foreach { open => open.attachTo(stack, open.result) }
					// Still returns the components as the children and not the wrappers
					ComponentWrapResult(stack, content.map { _.componentAndResult }, content.result)
				// Case: No segmentation used
				case None =>
					val components = content.map { _.componentAndResult }
					val stack = new ViewStack(parentHierarchy, components,
						axisPointer, layoutPointer, marginPointer, capPointer, customDrawers)
					content.foreach { open => open.attachTo(stack, open.result) }
					ComponentWrapResult(stack, components, content.result)
			}
		}
	}
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param axis Axis along which the items are placed.
	  *             X for horizontal rows, Y for vertical columns.
	  * @return Copy of this factory that uses the specified axis
	  */
	def withAxis(axis: Axis2D) = withAxisPointer(Fixed(axis))
	/**
	  * @param layout Layout to use on this stack
	  * @return Copy of this factory with the specified layout
	  */
	def withLayout(layout: StackLayout) = withLayoutPointer(Fixed(layout))
	/**
	  * @param margin Margins to place between each item in this stack
	  * @return Copy of this factory with the specified margin
	  */
	def withMargin(margin: StackLength) = withMarginPointer(Fixed(margin))
	/**
	  * @param cap Margin to place at each end of this stack
	  * @return Copy of this factory with the specified cap
	  */
	def withCap(cap: StackLength) = withCapPointer(Fixed(cap))
}

case class ViewStackFactory(parentHierarchy: ComponentHierarchy,
                            axisPointer: Changing[Axis2D] = Fixed(Y), layoutPointer: Changing[StackLayout] = Fixed(Fit),
                            marginPointer: Changing[StackLength] = Fixed(StackLength.any),
                            capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
                            customDrawers: Vector[CustomDrawer] = Vector(), segmentGroup: Option[SegmentGroup] = None)
	extends ViewStackFactoryLike[ViewStackFactory] with NonContextualViewContainerFactory[Stack, ReachComponentLike]
		with FromGenericContextFactory[BaseContext, ContextualViewStackFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withAxisPointer(p: Changing[Axis2D]): ViewStackFactory = copy(axisPointer = p)
	override def withLayoutPointer(p: Changing[StackLayout]): ViewStackFactory = copy(layoutPointer = p)
	override def withMarginPointer(p: Changing[StackLength]): ViewStackFactory = copy(marginPointer = p)
	override def withCapPointer(p: Changing[StackLength]): ViewStackFactory = copy(capPointer = p)
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ViewStackFactory =
		copy(customDrawers = drawers)
	
	override def withContext[N <: BaseContext](context: N) =
		ContextualViewStackFactory(parentHierarchy, context, axisPointer, layoutPointer, capPointer, customDrawers,
			segmentGroup)
}

case class ContextualViewStackFactory[+N <: BaseContext](parentHierarchy: ComponentHierarchy, context: N,
                                                         axisPointer: Changing[Axis2D] = Fixed(Y),
                                                         layoutPointer: Changing[StackLayout] = Fixed(Fit),
                                                         capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
                                                         customDrawers: Vector[CustomDrawer] = Vector(),
                                                         segmentGroup: Option[SegmentGroup] = None,
                                                         customMarginPointer: Option[Either[Changing[SizeCategory], Changing[StackLength]]] = None,
                                                         relatedFlag: Changing[Boolean] = AlwaysFalse)
	extends ViewStackFactoryLike[ContextualViewStackFactory[N]]
		with ContextualViewContainerFactory[N, BaseContext, Stack, ReachComponentLike, ContextualViewStackFactory]
{
	// COMPUTED --------------------------
	
	/**
	  * @return Copy of this factory that places the items close to each other
	  */
	def related = withRelatedFlag(AlwaysTrue)
	
	
	// IMPLEMENTED	--------------------------------
	
	override protected def marginPointer: Changing[StackLength] = customMarginPointer match {
		case Some(Left(sizePointer)) => sizePointer.map(context.scaledStackMargin)
		case Some(Right(pointer)) => pointer
		case None => relatedFlag.map { if (_) context.smallStackMargin else context.stackMargin }
	}
	
	override def withAxisPointer(p: Changing[Axis2D]): ContextualViewStackFactory[N] = copy(axisPointer = p)
	override def withLayoutPointer(p: Changing[StackLayout]): ContextualViewStackFactory[N] = copy(layoutPointer = p)
	override def withMarginPointer(p: Changing[StackLength]): ContextualViewStackFactory[N] =
		copy(customMarginPointer = Some(Right(p)))
	override def withCapPointer(p: Changing[StackLength]): ContextualViewStackFactory[N] = copy(capPointer = p)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualViewStackFactory[N] =
		copy(customDrawers = drawers)
	
	override def withContext[N2 <: BaseContext](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param p A pointer for margin sizes (general)
	  * @return Copy of this factory with that pointer in use
	  */
	def withMarginSizePointer(p: Changing[SizeCategory]) =
		copy(customMarginPointer = Some(Left(p)))
	/**
	  * @param p A pointer for stack cap sizes (general)
	  * @return Copy of this factory with that pointer in use
	  */
	def withCapSizePointer(p: Changing[SizeCategory]) =
		copy(capPointer = p.map(context.scaledStackMargin))
	/**
	  * @param flag A pointer that indicates whether the components
	  *          should be placed close to each other (true) or at the default distance (false)
	  * @return Copy of this factory with that pointer in use
	  */
	def withRelatedFlag(flag: Changing[Boolean]) = copy(relatedFlag = flag)
	
	/**
	  * @param margin Margin to place between items (general)
	  * @return Copy of this factory with those margins
	  */
	def withMargin(margin: SizeCategory) = withMarginSizePointer(Fixed(margin))
	/**
	  * @param cap Cap to place at each end of this stack
	  * @return Copy of this factory with that cap margin
	  */
	def withCap(cap: SizeCategory) = withCapSizePointer(Fixed(cap))
}

/**
  * A pointer-based stack that adds and removes items based on activation pointer events
  * @author Mikko Hilpinen
  * @since 14.11.2020, v0.1
  */
// TODO: Create a variant that works with static components
class ViewStack(override val parentHierarchy: ComponentHierarchy,
                componentData: Vector[(ReachComponentLike, Changing[Boolean])],
                directionPointer: Changing[Axis2D] = Fixed(Y), layoutPointer: Changing[StackLayout] = Fixed(Fit),
                marginPointer: Changing[StackLength] = Fixed(StackLength.any),
                capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
                override val customDrawers: Vector[CustomDrawer] = Vector())
	extends Stack
{
	// ATTRIBUTES	-------------------------------
	
	private val activeComponentsCache = ResettableLazy {
		componentData.flatMap { case (component, pointer) =>
			if (pointer.value) Some(component) else None
		}
	}
	
	private val revalidateOnChange = ChangeListener.onAnyChange {
		revalidate()
		DetachmentChoice.continue
	}
	private lazy val resetActiveComponentsOnChange = ChangeListener.onAnyChange {
		activeComponentsCache.reset()
		revalidate()
		DetachmentChoice.continue
	}
	
	/**
	  * A pointer to this stack's visibility state.
	  * This stack is visible while there is one or more components visible inside.
	  */
	override lazy val visibilityPointer = {
		val pointers = componentData.map { _._2 }
		if (pointers.isEmpty)
			AlwaysFalse
		else if (pointers.exists { _.isAlwaysTrue })
			AlwaysTrue
		else
			pointers.reduce { _ || _ }
	}
	
	
	// INITIAL CODE	-------------------------------
	
	// Updates components list when component pointers get updated
	componentData.map { _._2 }.foreach { _.addListener(resetActiveComponentsOnChange) }
	// Revalidates this component on other layout changes
	directionPointer.addListener(revalidateOnChange)
	layoutPointer.addListener(revalidateOnChange)
	marginPointer.addListener(revalidateOnChange)
	capPointer.addListener(revalidateOnChange)
	
	
	// IMPLEMENTED	-------------------------------
	
	override def direction = directionPointer.value
	
	override def layout = layoutPointer.value
	
	override def margin = marginPointer.value
	
	override def cap = capPointer.value
	
	override def components = activeComponentsCache.value
}
