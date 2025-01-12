package utopia.reach.container.multi

import utopia.firmament.component.container.many.StackLike
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout.{Center, Fit, Leading, Trailing}
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.operator.sign.Sign.Negative
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Alignment, Axis, Axis2D}
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

/**
  * Common trait for stack factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait StackSettingsLike[+Repr] extends CustomDrawableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The axis along which the items in the stacks are placed.
	  * Y yields columns and X yields rows.
	  */
	def axis: Axis2D
	/**
	  * Layout that determines how the components are sized perpendicular to stack axis.
	  * E.g. for columns, this property defines horizontal component placement and width.
	  */
	def layout: StackLayout
	/**
	  * Specifies the margin placed at each end of the created stacks
	  */
	def capPointer: Changing[StackLength]
	
	/**
	  * The axis along which the items in the stacks are placed.
	  * Y yields columns and X yields rows.
	  * @param axis New axis to use.
	  *             The axis along which the items in the stacks are placed.
	  *             Y yields columns and X yields rows.
	  * @return Copy of this factory with the specified axis
	  */
	def withAxis(axis: Axis2D): Repr
	/**
	  * Specifies the margin placed at each end of the created stacks
	  * @param p Pointer that contains the applied "cap" length.
	  *          Cap is the margin placed at each end of the created stacks.
	  * @return Copy of this factory with the specified cap
	  */
	def withCapPointer(p: Changing[StackLength]): Repr
	/**
	  * Layout that determines how the components are sized perpendicular to stack axis.
	  * E.g. for columns, this property defines horizontal component placement and width.
	  * @param layout New layout to use.
	  *               Layout that determines how the components are sized perpendicular to stack axis.
	  *               E.g. for columns, this property defines horizontal component placement and width.
	  * @return Copy of this factory with the specified layout
	  */
	def withLayout(layout: StackLayout): Repr
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return A copy of this factory that builds columns
	  */
	def column = withAxis(Y)
	/**
	  * @return A copy of this factory that builds rows
	  */
	def row = withAxis(X)
	
	/**
	  * @return A copy of this factory with center layout
	  */
	def centered = withLayout(Center)
	def leading = withLayout(Leading)
	def trailing = withLayout(Trailing)
	
	
	// OTHER	--------------------
	
	/**
	  * Specifies the margin placed at each end of the created stacks
	  * @param cap New cap to use.
	  *            Specifies the margin placed at each end of the created stacks
	  * @return Copy of this factory with the specified cap
	  */
	def withCap(cap: StackLength): Repr = withCapPointer(Fixed(cap))
	/**
	  * @param f A mapping function applied to this stack's cap
	  * @return Copy of this stack with modified "cap" value(s)
	  */
	def mapCap(f: StackLength => StackLength) = withCapPointer(capPointer.map(f))
	/**
	  * @param f A mapping function applied to this stack's cap. Yields potentially variable results.
	  * @return Copy of this stack with modified "cap" value(s)
	  */
	def flatMapCap(f: StackLength => Changing[StackLength]) = withCapPointer(capPointer.flatMap(f))
}

object StackSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing stacks
  * @param customDrawers Custom drawers to assign to created components
  * @param axis          The axis along which the items in the stacks are placed.
  *                      Y yields columns and X yields rows.
  * @param layout        Layout that determines how the components are sized perpendicular to stack axis.
  *                      E.g. for columns, this property defines horizontal component placement and width.
  * @param capPointer           Specifies the margin placed at each end of the created stacks. Variable.
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class StackSettings(customDrawers: Seq[CustomDrawer] = Empty, axis: Axis2D = Axis.Y,
                         layout: StackLayout = StackLayout.Fit,
                         capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero))
	extends StackSettingsLike[StackSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withAxis(axis: Axis2D) = copy(axis = axis)
	override def withCapPointer(p: Changing[StackLength]): StackSettings = copy(capPointer = p)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
	override def withLayout(layout: StackLayout) = copy(layout = layout)
}

/**
  * Common trait for factories that wrap a stack settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait StackSettingsWrapper[+Repr] extends StackSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: StackSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: StackSettings): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this factory that builds centered rows
	  */
	def centeredRow = withAxisAndLayout(X, Center)
	
	
	// IMPLEMENTED	--------------------
	
	override def axis = settings.axis
	override def capPointer: Changing[StackLength] = settings.capPointer
	override def customDrawers = settings.customDrawers
	override def layout = settings.layout
	
	override def withAxis(axis: Axis2D) = mapSettings { _.withAxis(axis) }
	override def withCapPointer(p: Changing[StackLength]): Repr = mapSettings { _.withCapPointer(p) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withLayout(layout: StackLayout) = mapSettings { _.withLayout(layout) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: StackSettings => StackSettings) = withSettings(f(settings))
	
	def withAxisAndLayout(axis: Axis2D, layout: StackLayout): Repr =
		mapSettings { _.copy(axis = axis, layout = layout) }
}

/**
  * Common trait for factories that are used for constructing stacks
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait StackFactoryLike[+Repr <: StackFactoryLike[_]]
	extends StackSettingsWrapper[Repr] with CombiningContainerFactory[Stack, ReachComponentLike]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return A pointer that determines the margin to place between the items in created stacks
	  */
	def marginPointer: Changing[StackLength]
	
	/**
	  * @param p Pointer that determines the margin to place between the items in this stack
	  * @return A copy of this factory with the specified margin
	  */
	def withMarginPointer(p: Changing[StackLength]): Repr
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return A copy of this factory that doesn't allow any margins
	  */
	def withoutMargin = withMargin(StackLength.fixedZero)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def apply[C <: ReachComponentLike, R](content: BundledOpenComponents[C, R]): ComponentsWrapResult[Stack, C, R] = {
		val stack: Stack = new _Stack(parentHierarchy, content.component, axis, layout, marginPointer, capPointer,
			customDrawers)
		content attachTo stack
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param margin Margin to place between the items in this stack
	  * @return A copy of this factory with the specified margin
	  */
	def withMargin(margin: StackLength): Repr = withMarginPointer(Fixed(margin))
	
	/**
	  * Creates a new stack using the specified components as segments
	  * @param content The components to place within this stack, in open form
	  * @param group The group within which the segments shall be placed
	  * @tparam C Type of the placed components
	  * @tparam R Type of additional results for each component
	  * @return A new segmented stack
	  */
	def segmented[C <: ReachComponentLike, R](content: Seq[OpenComponent[C, R]], group: SegmentGroup) = {
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
	@deprecated("Renamed to .segmented(...)", "v1.1")
	def withSegments[C <: ReachComponentLike, R](content: Seq[OpenComponent[C, R]], group: SegmentGroup) =
		segmented(content, group)
	
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
	def pair[C <: ReachComponentLike, R](content: OpenComponent[Pair[C], R], alignment: Alignment = Alignment.Left,
	                                     forceFitLayout: Boolean = false): ComponentWrapResult[Stack, Seq[C], R] =
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
			case None => (Y, alignment.vertical.sign.binaryOr(Negative), Center)
		}
		// Negative sign keeps order, positive swaps it
		val orderedContent = content.mapComponent { _ * -sign }
		// Creates the stack
		withAxisAndLayout(axis, if (forceFitLayout) Fit else layout)(orderedContent)
	}
	@deprecated("Renamed to .pair(...)", "v1.1")
	def forPair[C <: ReachComponentLike, R](content: OpenComponent[Pair[C], R], alignment: Alignment = Alignment.Left,
	                                        forceFitLayout: Boolean = false): ComponentWrapResult[Stack, Seq[C], R] =
		pair(content, alignment, forceFitLayout)
}

/**
  * Factory class that is used for constructing stacks without using contextual information
  * @param marginPointer A pointer that determines the amount of empty space between adjacent items in the created stacks
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class StackFactory(parentHierarchy: ComponentHierarchy, settings: StackSettings = StackSettings.default,
                        marginPointer: Changing[StackLength] = Fixed(StackLength.any))
	extends StackFactoryLike[StackFactory]
		with FromGenericContextFactory[BaseContextPropsView, ContextualStackFactory]
		with NonContextualCombiningContainerFactory[Stack, ReachComponentLike]
{
	// IMPLEMENTED  ------------------------
	
	override def withContext[N <: BaseContextPropsView](context: N): ContextualStackFactory[N] =
		ContextualStackFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: StackSettings): StackFactory = copy(settings = settings)
	override def withMarginPointer(p: Changing[StackLength]): StackFactory = copy(marginPointer = p)
	
	
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
		segmented(content.component, group).withResult(content.result)
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
		pair(Open.using(contentFactory)(fill), alignment, forceFitLayout)
}

/**
  * Factory class used for constructing stacks using contextual component creation information
  * @param relatedFlag A pointer that contains true when the items in the created stacks should be considered closely related to each other,
  *                   resulting in a smaller margin placed between them.
  * @tparam N Type of context used and passed along by this factory
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class ContextualStackFactory[+N <: BaseContextPropsView](parentHierarchy: ComponentHierarchy, context: N,
                                                              settings: StackSettings = StackSettings.default,
                                                              customMarginPointer: Option[Changing[StackLength]] = None,
                                                              relatedFlag: Flag = AlwaysFalse)
	extends StackFactoryLike[ContextualStackFactory[N]]
		with ContextualCombiningContainerFactory[N, BaseContextPropsView, Stack, ReachComponentLike, ContextualStackFactory]
{
	// ATTRIBUTES   ---------------------------
	
	override lazy val marginPointer: Changing[StackLength] =
		customMarginPointer.getOrElse { context.stackMarginPointerFor(relatedFlag) }
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return Copy of this factory that places the items close to each other
	  */
	def related = copy(relatedFlag = AlwaysTrue)
	/**
	  * @return Copy of this factory that places the items at the default distance from each other
	  */
	def unrelated = copy(relatedFlag = AlwaysFalse)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def withMarginPointer(p: Changing[StackLength]): ContextualStackFactory[N] =
		copy(customMarginPointer = Some(p))
	
	override def withSettings(settings: StackSettings): ContextualStackFactory[N] = copy(settings = settings)
	override def withContext[N2 <: BaseContextPropsView](newContext: N2): ContextualStackFactory[N2] =
		copy(context = newContext)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param margin New size of margins to use (general)
	  * @return Copy of this factory that uses the specified margin size
	  */
	def withMargin(margin: SizeCategory): ContextualStackFactory[N] =
		withMarginPointer(context.scaledStackMarginPointer(margin))
	/**
	  * @param margin Size of margins to apply. None if no margins should be added at all.
	  * @return Copy of this factory with the specified margin size.
	  */
	def withMargin(margin: Option[SizeCategory]): ContextualStackFactory[N] = margin match {
		case Some(margin) => withMargin(margin)
		case None => withoutMargin
	}
	/**
	  * @param cap New size of margins to place at each end of this stack (general)
	  * @return Copy of this factory that uses the specified cap size
	  */
	def withCap(cap: SizeCategory): ContextualStackFactory[N] = withCapPointer(context.scaledStackMarginPointer(cap))
	
	/**
	  * @param relatedFlag A flag that contains true when the items in this stack are to be considered closely related,
	  *                    resulting in a smaller margin between them.
	  * @return Copy of this stack with the specified "are related" flag applied.
	  */
	def withRelatedFlag(relatedFlag: Flag) = copy(relatedFlag = relatedFlag)
	
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
		segmented(content.component, group).withResult(content.result)
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
		pair(Open.withContext(context)(contentFactory)(fill), alignment, forceFitLayout)
}

/**
  * Used for defining stack creation settings outside the component building process
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class StackSetup(settings: StackSettings = StackSettings.default)
	extends StackSettingsWrapper[StackSetup] with Cff[StackFactory]
		with Gccff[BaseContextPropsView, ContextualStackFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = StackFactory(hierarchy, settings)
	
	override def withContext[N <: BaseContextPropsView](hierarchy: ComponentHierarchy, context: N): ContextualStackFactory[N] =
		ContextualStackFactory(hierarchy, context, settings)
	
	override def withSettings(settings: StackSettings) = copy(settings = settings)
}

object Stack extends StackSetup()
{
	// OTHER	--------------------
	
	def apply(settings: StackSettings) = withSettings(settings)
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
	def visibilityPointer: Flag
	
	
	// IMPLEMENTED  ----------------------
	
	override def children = components
}

private class _Stack(override val parentHierarchy: ComponentHierarchy,
                     override val components: Seq[ReachComponentLike], override val direction: Axis2D,
                     override val layout: StackLayout, marginPointer: Changing[StackLength],
                     capPointer: Changing[StackLength], override val customDrawers: Seq[CustomDrawer])
	extends Stack
{
	// ATTRIBUTES   ---------------------------
	
	override lazy val visibilityPointer: Flag = if (components.isEmpty) AlwaysFalse else AlwaysTrue
	
	
	// INITIAL CODE ---------------------------
	
	marginPointer.addListenerWhile(linkedFlag) { _ => revalidate() }
	capPointer.addListenerWhile(linkedFlag) { _ => revalidate() }
	
	
	// IMPLEMENTED  ---------------------------
	
	override def margin: StackLength = marginPointer.value
	override def cap: StackLength = capPointer.value
}