package utopia.reach.container.multi

import utopia.firmament.component.container.many.StackLike
import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout.{Center, Fit, Leading, Trailing}
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.Pair
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}
import utopia.flow.view.template.eventful.FlagLike
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Alignment, Axis2D}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.ComponentCreationResult.ComponentsResult
import utopia.reach.component.wrapper.ComponentWrapResult.ComponentsWrapResult
import utopia.reach.component.wrapper.OpenComponent.BundledOpenComponents
import utopia.reach.component.wrapper.{ComponentCreationResult, ComponentWrapResult, Open, OpenComponent}

object Stack extends Cff[StackFactory] with Gccff[BaseContext, ContextualStackFactory]
{
	// IMPLEMENTED  ----------------------
	
	override def apply(hierarchy: ComponentHierarchy) = StackFactory(hierarchy)
	
	override def withContext[N <: BaseContext](parentHierarchy: ComponentHierarchy, context: N): ContextualStackFactory[N] =
		ContextualStackFactory(parentHierarchy, context)
}

trait StackFactoryLike[+Repr <: StackFactoryLike[_]]
	extends CombiningContainerFactory[Stack, ReachComponentLike] with CustomDrawableFactory[Repr]
{
	// ABSTRACT --------------------------
	
	def axis: Axis2D
	def layout: StackLayout
	def margin: StackLength
	def cap: StackLength
	
	/**
	  * @param axis New axis, along which this stack places the components.
	  *             X = horizontal stack (row) & Y = vertical stack (column)
	  * @return A copy of this factory with the specified axis
	  */
	def withAxis(axis: Axis2D): Repr
	/**
	  * @param layout The layout to use in this stack. Affects the breadth of the placed components.
	  * @return A copy of this factory with the specified layout.
	  */
	def withLayout(layout: StackLayout): Repr
	/**
	  * @param margin Margin to place between the items in this stack
	  * @return A copy of this factory with the specified margin
	  */
	def withMargin(margin: StackLength): Repr
	/**
	  * @param cap The cap to place at each end of this stack
	  * @return A copy of this factory with the specified cap
	  */
	def withCap(cap: StackLength): Repr
	
	def withAxisAndLayout(axis: Axis2D, layout: StackLayout): Repr
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return A copy of this factory that builds columns
	  */
	def column = withAxis(Y)
	/**
	  * @return A copy of this factory that builds rows
	  */
	def row = withAxis(X)
	
	/**
	  * @return A copy of this factory that doesn't allow any margins
	  */
	def withoutMargin = withMargin(StackLength.fixedZero)
	
	/**
	  * @return A copy of this factory with center layout
	  */
	def centered = withLayout(Center)
	
	/**
	  * @return A copy of this factory that builds centered rows
	  */
	def centeredRow = withAxisAndLayout(X, Center)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def apply[C <: ReachComponentLike, R](content: BundledOpenComponents[C, R]): ComponentsWrapResult[Stack, C, R] = {
		val stack: Stack = new _Stack(parentHierarchy, content.component, axis, layout, margin, cap,
			customDrawers)
		content attachTo stack
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new stack using the specified components as segments
	  * @param content The components to place within this stack, in open form
	  * @param group The group within which the segments shall be placed
	  * @tparam C Type of the placed components
	  * @tparam R Type of additional results for each component
	  * @return A new segmented stack
	  */
	def withSegments[C <: ReachComponentLike, R](content: Vector[OpenComponent[C, R]], group: SegmentGroup) = {
		// Wraps the components in segments first
		val wrapped = Open { hierarchy =>
			val wrapResult = group.wrapUnderSingle(hierarchy, content)
			wrapResult.map { _.parent } -> wrapResult.map { _.result }
		}
		// The specified group defines the direction of this stack
		val stack = withAxis(group.rowDirection)(wrapped)
		// Returns with the original components as children (even though the segments are the real children)
		stack.withChild(content.map { _.component })
	}
	
	/**
	  * Creates a new stack that contains two items
	  * @param content        Items to place in this stack
	  * @param alignment      Alignment to use when placing the items.
	  *                       The direction of the alignment determines the
	  *                       position of the 'first' item in the 'content'.
	  *
	  *                       Eg. Left alignment means that the first item will be
	  *                       placed at the left side and the second item on the right.
	  *                       Bottom alignment means that the first item will be placed at the bottom and the second at
	  *                       the top.
	  *
	  *                       Default = Left
	  *
	  * @param forceFitLayout Whether layout should always be set to <i>Fit</i>, regardless of alignment
	  * @tparam C Type of the components
	  * @tparam R Type of additional creation result
	  * @return A new stack with the two items in it
	  */
	def forPair[C <: ReachComponentLike, R](content: OpenComponent[Pair[C], R], alignment: Alignment = Alignment.Left,
	                                         forceFitLayout: Boolean = false): ComponentWrapResult[Stack, Vector[C], R] =
	{
		// Specifies stack axis, layout and item order based on the alignment
		// The first item always goes to the direction of the alignment
		// (Eg. Left = first, then second (centered vertically), Bottom = second (centered horizontally), then first)
		val (axis, sign, layout) = alignment.horizontalDirection match {
			case Some(horizontal) =>
				val layout = alignment.verticalDirection match {
					case Some(vertical) =>
						vertical match {
							case Down => Trailing
							case Up => Leading
						}
					case None => Center
				}
				(X, horizontal.sign, layout)
			case None => (Y, alignment.vertical.sign, Center)
		}
		// Negative sign keeps order, positive swaps it
		val orderedContent = content.mapComponent { pair => (pair * -sign).toVector }
		// Creates the stack
		withAxisAndLayout(axis, if (forceFitLayout) Fit else layout)(orderedContent)
	}
}

case class StackFactory(parentHierarchy: ComponentHierarchy, axis: Axis2D = Y, layout: StackLayout = Fit,
                        margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixedZero,
                        customDrawers: Vector[CustomDrawer] = Vector())
	extends StackFactoryLike[StackFactory]
		with NonContextualCombiningContainerFactory[Stack, ReachComponentLike]
		with FromGenericContextFactory[BaseContext, ContextualStackFactory]
{
	// IMPLEMENTED  ------------------------
	
	override def withContext[N <: BaseContext](context: N): ContextualStackFactory[N] =
		ContextualStackFactory(parentHierarchy, context, axis, layout, cap, customDrawers, None)
	
	override def withAxis(axis: Axis2D): StackFactory = copy(axis = axis)
	override def withLayout(layout: StackLayout): StackFactory = copy(layout = layout)
	override def withMargin(margin: StackLength): StackFactory = copy(margin = margin)
	override def withCap(cap: StackLength): StackFactory = copy(cap = cap)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): StackFactory =
		copy(customDrawers = drawers)
	
	override def withAxisAndLayout(axis: Axis2D, layout: StackLayout): StackFactory =
		copy(axis = axis, layout = layout)
	
	
	// OTHER    ---------------------------
	
	/**
	  * Builds a segmented stack. Segmented means that the size of the contents is adjusted to match parallel
	  * stacks or other components.
	  * @param contentFactory Factory used for creating the components
	  * @param group   The group within which the segments shall be placed
	  * @param fill A function that accepts an iterator that yields new component factories.
	  *             Yields the components to place in this stack.
	  *             The components should be returned in the same order as the
	  *             factories were acquired from the iterator.
	  * @tparam C Type of the placed components
	  * @tparam R Type of additional results for each component
	  * @return A new segmented stack
	  */
	def buildSegmented[F, C <: ReachComponentLike, R](contentFactory: Cff[F], group: SegmentGroup)
	                                                 (fill: Iterator[F] => ComponentsResult[C, R]) =
	{
		val content = Open.manyUsing(contentFactory) { fill(_) }
		withSegments(content.component, group).withResult(content.result)
	}
	
	/**
	  * Creates a new stack that contains two items
	  * @param contentFactory A factory used for building the contents of this stack
	  * @param alignment      Alignment to use when placing the items.
	  *                       The direction of the alignment determines the
	  *                       position of the 'first' item in the 'content'.
	  *
	  *                       Eg. Left alignment means that the first item will be
	  *                       placed at the left side and the second item on the right.
	  *                       Bottom alignment means that the first item will be placed at the bottom and the second at
	  *                       the top.
	  *
	  *                       Default = Left
	  *
	  * @param forceFitLayout Whether layout should always be set to Fit, regardless of alignment
	  * @param fill A function that accepts an initialized component factory and yields two components
	  * @tparam C Type of the components
	  * @tparam R Type of additional creation result
	  * @return A new stack with the two items in it
	  */
	def buildPair[F, C <: ReachComponentLike, R](contentFactory: Cff[F], alignment: Alignment = Alignment.Left,
	                                             forceFitLayout: Boolean = false)
	                                            (fill: F => ComponentCreationResult[Pair[C], R]) =
		forPair(Open.using(contentFactory)(fill), alignment, forceFitLayout)
}

case class ContextualStackFactory[+N <: BaseContext](parentHierarchy: ComponentHierarchy, context: N,
                                                    axis: Axis2D = Y, layout: StackLayout = Fit,
                                                    cap: StackLength = StackLength.fixedZero,
                                                    customDrawers: Vector[CustomDrawer] = Vector(),
                                                    customMargin: Option[StackLength] = None,
                                                    areRelated: Boolean = false)
	extends StackFactoryLike[ContextualStackFactory[N]]
		with ContextualCombiningContainerFactory[N, BaseContext, Stack, ReachComponentLike, ContextualStackFactory]
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Copy of this factory that places the items close to each other
	  */
	def related = copy(areRelated = true)
	/**
	  * @return Copy of this factory that places the items at the default distance from each other
	  */
	def unrelated = copy(areRelated = false)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def margin: StackLength = customMargin.getOrElse {
		if (areRelated) context.smallStackMargin else context.stackMargin
	}
	
	override def withContext[N2 <: BaseContext](newContext: N2): ContextualStackFactory[N2] =
		copy(context = newContext)
	
	override def withAxis(axis: Axis2D) = copy(axis = axis)
	override def withLayout(layout: StackLayout) = copy(layout = layout)
	override def withMargin(margin: StackLength) =
		copy(customMargin = Some(margin))
	override def withCap(cap: StackLength) = copy(cap = cap)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		copy(customDrawers = drawers)
	
	override def withAxisAndLayout(axis: Axis2D, layout: StackLayout) =
		copy(axis = axis, layout = layout)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param margin New size of margins to use (general)
	  * @return Copy of this factory that uses the specified margin size
	  */
	def withMargin(margin: SizeCategory): ContextualStackFactory[N] = withMargin(context.margins.around(margin))
	/**
	  * @param cap New size of margins to place at each end of this stack (general)
	  * @return Copy of this factory that uses the specified cap size
	  */
	def withCap(cap: SizeCategory): ContextualStackFactory[N] = withCap(context.margins.around(cap))
	
	/**
	  * Builds a segmented stack. Segmented means that the size of the contents is adjusted to match parallel
	  * stacks or other components.
	  * @param contentFactory Factory used for creating the components
	  * @param group          The group within which the segments shall be placed
	  * @param fill           A function that accepts an iterator that yields new component factories.
	  *                       Yields the components to place in this stack.
	  *                       The components should be returned in the same order as the
	  *                       factories were acquired from the iterator.
	  * @tparam C Type of the placed components
	  * @tparam R Type of additional results for each component
	  * @return A new segmented stack
	  */
	def buildSegmented[F, C <: ReachComponentLike, R](contentFactory: Ccff[N, F], group: SegmentGroup)
	                                                 (fill: Iterator[F] => ComponentsResult[C, R]) =
	{
		val content = Open.withContext(context).many(contentFactory) { fill(_) }
		withSegments(content.component, group).withResult(content.result)
	}
	
	/**
	  * Creates a new stack that contains two items
	  * @param contentFactory A factory used for building the contents of this stack
	  * @param alignment      Alignment to use when placing the items.
	  *                       The direction of the alignment determines the
	  *                       position of the 'first' item in the 'content'.
	  *
	  *                       Eg. Left alignment means that the first item will be
	  *                       placed at the left side and the second item on the right.
	  *                       Bottom alignment means that the first item will be placed at the bottom and the second at
	  *                       the top.
	  *
	  *                       Default = Left
	  *
	  * @param forceFitLayout Whether layout should always be set to Fit, regardless of alignment
	  * @param fill           A function that accepts an initialized component factory and yields two components
	  * @tparam C Type of the components
	  * @tparam R Type of additional creation result
	  * @return A new stack with the two items in it
	  */
	def buildPair[F, C <: ReachComponentLike, R](contentFactory: Ccff[N, F], alignment: Alignment = Alignment.Left,
	                                             forceFitLayout: Boolean = false)
	                                            (fill: F => ComponentCreationResult[Pair[C], R]) =
		forPair(Open.withContext(context)(contentFactory)(fill), alignment, forceFitLayout)
}

/**
  * Common trait for all Reach stack implementations, regardless of implementation style
  */
trait Stack extends CustomDrawReachComponent with StackLike[ReachComponentLike]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return A pointer that contains true while this stack should be displayed. I.e. is non-empty.
	  */
	def visibilityPointer: FlagLike
	
	
	// IMPLEMENTED  ----------------------
	
	override def children = components
}

private class _Stack(override val parentHierarchy: ComponentHierarchy,
                     override val components: Vector[ReachComponentLike], override val direction: Axis2D,
                     override val layout: StackLayout, override val margin: StackLength,
                     override val cap: StackLength, override val customDrawers: Vector[CustomDrawer])
	extends Stack
{
	override lazy val visibilityPointer: FlagLike = if (components.isEmpty) AlwaysFalse else AlwaysTrue
}