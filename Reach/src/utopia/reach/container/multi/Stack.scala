package utopia.reach.container.multi

import utopia.firmament.component.container.many.StackLike
import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout.{Center, Fit, Leading, Trailing}
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.sign.Sign.Negative
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}
import utopia.flow.view.template.eventful.FlagLike
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
	def cap: StackLength
	
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
	  * @param cap New cap to use.
	  *            Specifies the margin placed at each end of the created stacks
	  * @return Copy of this factory with the specified cap
	  */
	def withCap(cap: StackLength): Repr
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
	
	def mapCap(f: StackLength => StackLength) = withCap(f(cap))
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
  * @param cap           Specifies the margin placed at each end of the created stacks
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class StackSettings(customDrawers: Vector[CustomDrawer] = Vector.empty, axis: Axis2D = Axis.Y,
                         layout: StackLayout = StackLayout.Fit, cap: StackLength = StackLength.fixedZero)
	extends StackSettingsLike[StackSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withAxis(axis: Axis2D) = copy(axis = axis)
	override def withCap(cap: StackLength) = copy(cap = cap)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) = copy(customDrawers = drawers)
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
	override def cap = settings.cap
	override def customDrawers = settings.customDrawers
	override def layout = settings.layout
	
	override def withAxis(axis: Axis2D) = mapSettings { _.withAxis(axis) }
	override def withCap(cap: StackLength) = mapSettings { _.withCap(cap) }
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
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
	  * @return Margin to place between the items in created stacks
	  */
	def margin: StackLength
	
	/**
	  * @param margin Margin to place between the items in this stack
	  * @return A copy of this factory with the specified margin
	  */
	def withMargin(margin: StackLength): Repr
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return A copy of this factory that doesn't allow any margins
	  */
	def withoutMargin = withMargin(StackLength.fixedZero)
	
	
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
	def segmented[C <: ReachComponentLike, R](content: Vector[OpenComponent[C, R]], group: SegmentGroup) = {
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
	def withSegments[C <: ReachComponentLike, R](content: Vector[OpenComponent[C, R]], group: SegmentGroup) =
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
			case None => (Y, alignment.vertical.sign.binaryOr(Negative), Center)
		}
		// Negative sign keeps order, positive swaps it
		val orderedContent = content.mapComponent { pair => (pair * -sign).toVector }
		// Creates the stack
		withAxisAndLayout(axis, if (forceFitLayout) Fit else layout)(orderedContent)
	}
	@deprecated("Renamed to .pair(...)", "v1.1")
	def forPair[C <: ReachComponentLike, R](content: OpenComponent[Pair[C], R], alignment: Alignment = Alignment.Left,
	                                        forceFitLayout: Boolean = false): ComponentWrapResult[Stack, Vector[C], R] =
		pair(content, alignment, forceFitLayout)
}

/**
  * Factory class that is used for constructing stacks without using contextual information
  * @param margin Amount of empty space between adjacent items in the created stacks
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class StackFactory(parentHierarchy: ComponentHierarchy, settings: StackSettings = StackSettings.default,
                        margin: StackLength = StackLength.any)
	extends StackFactoryLike[StackFactory]
		with FromGenericContextFactory[BaseContext, ContextualStackFactory]
		with NonContextualCombiningContainerFactory[Stack, ReachComponentLike]
{
	// IMPLEMENTED  ------------------------
	
	override def withContext[N <: BaseContext](context: N): ContextualStackFactory[N] =
		ContextualStackFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: StackSettings): StackFactory = copy(settings = settings)
	override def withMargin(margin: StackLength): StackFactory = copy(margin = margin)
	
	
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
  * @param areRelated Whether the items in the created stacks should be considered closely related to each other,
  *                   resulting in a smaller margin placed between them.
  * @tparam N Type of context used and passed along by this factory
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class ContextualStackFactory[+N <: BaseContext](parentHierarchy: ComponentHierarchy, context: N,
                                                     settings: StackSettings = StackSettings.default,
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
	
	override def withSettings(settings: StackSettings): ContextualStackFactory[N] = copy(settings = settings)
	override def withContext[N2 <: BaseContext](newContext: N2): ContextualStackFactory[N2] =
		copy(context = newContext)
	override def withMargin(margin: StackLength) =
		copy(customMargin = Some(margin))
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param margin New size of margins to use (general)
	  * @return Copy of this factory that uses the specified margin size
	  */
	def withMargin(margin: SizeCategory): ContextualStackFactory[N] = withMargin(context.scaledStackMargin(margin))
	/**
	  * @param cap New size of margins to place at each end of this stack (general)
	  * @return Copy of this factory that uses the specified cap size
	  */
	def withCap(cap: SizeCategory): ContextualStackFactory[N] = withCap(context.scaledStackMargin(cap))
	
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
  * Used for defining stack creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class StackSetup(settings: StackSettings = StackSettings.default)
	extends StackSettingsWrapper[StackSetup] with Cff[StackFactory] with Gccff[BaseContext, ContextualStackFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = StackFactory(hierarchy, settings)
	
	override def withContext[N <: BaseContext](hierarchy: ComponentHierarchy, context: N): ContextualStackFactory[N] =
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