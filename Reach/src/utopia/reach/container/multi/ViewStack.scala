package utopia.reach.container.multi

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout.{Center, Leading, Trailing}
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.event.listener.ChangeListener
import utopia.flow.util.{Mutate, NotEmpty}
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.template.eventful.Flag.wrap
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.enumeration.{Axis, Axis2D}
import utopia.reach.component.factory.{ComponentFactoryFactory, FromGenericContextComponentFactoryFactory, FromGenericContextFactory}
import utopia.reach.component.hierarchy.{ComponentHierarchy, SeedHierarchyBlock}
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentLike}
import utopia.reach.component.wrapper.ComponentWrapResult.SwitchableComponentsWrapResult
import utopia.reach.component.wrapper.OpenComponent.{SeparateOpenComponents, SwitchableOpenComponents}
import utopia.reach.component.wrapper.{ComponentCreationResult, ComponentWrapResult, Open, OpenComponent}

/**
  * Common trait for view stack factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 12.01.2025, v1.5
  */
trait ViewStackSettingsLike[+Repr] extends CustomDrawableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * A pointer that determines the axis along which the items in the stacks are placed.
	  * Y yields columns and X yields rows.
	  */
	def axisPointer: Changing[Axis2D]
	/**
	  * A pointer that determines how the components are sized perpendicular to the stack axis.
	  * E.g. for columns, defines horizontal component placement and width.
	  */
	def layoutPointer: Changing[StackLayout]
	/**
	  * A pointer that specifies the margin placed at each end of the created stacks
	  */
	def capPointer: Changing[StackLength]
	/**
	  * A segment group in which all the created components will be placed. Used to align the
	  * components with those of other stacks.
	  * None if segmentation is not used.
	  */
	def segmentGroup: Option[SegmentGroup]
	
	/**
	  * A pointer that determines the axis along which the items in the stacks are placed.
	  * Y yields columns and X yields rows.
	  * @param p New axis pointer to use.
	  *          A pointer that determines the axis along which the items in the stacks are placed.
	  *          Y yields columns and X yields rows.
	  * @return Copy of this factory with the specified axis pointer
	  */
	def withAxisPointer(p: Changing[Axis2D]): Repr
	/**
	  * A pointer that specifies the margin placed at each end of the created stacks
	  * @param p New cap pointer to use.
	  *          A pointer that specifies the margin placed at each end of the created stacks
	  * @return Copy of this factory with the specified cap pointer
	  */
	def withCapPointer(p: Changing[StackLength]): Repr
	/**
	  * A pointer that determines how the components are sized perpendicular to the stack axis.
	  * E.g. for columns, defines horizontal component placement and width.
	  * @param p New layout pointer to use.
	  *          A pointer that determines how the components are sized perpendicular to the stack
	  *          axis.
	  *          E.g. for columns, defines horizontal component placement and width.
	  * @return Copy of this factory with the specified layout pointer
	  */
	def withLayoutPointer(p: Changing[StackLayout]): Repr
	/**
	  * A segment group in which all the created components will be placed. Used to align the
	  * components with those of other stacks.
	  * None if segmentation is not used.
	  * @param group New segment group to use.
	  *              A segment group in which all the created components will be placed. Used to
	  *              align the components with those of other stacks.
	  *              None if segmentation is not used.
	  * @return Copy of this factory with the specified segment group
	  */
	def withSegmentGroup(group: Option[SegmentGroup]): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * Copy of this factory that builds rows
	  */
	def row = withAxis(X)
	
	/**
	  * Copy of this factory where items are centered
	  */
	def centered = withLayout(Center)
	def leading = withLayout(Leading)
	def trailing = withLayout(Trailing)
	
	
	// OTHER	--------------------
	
	/**
	  * @param axis Axis along which the items are placed.
	  *             X for horizontal rows, Y for vertical columns.
	  * @return Copy of this factory that uses the specified axis
	  */
	def withAxis(axis: Axis2D) = withAxisPointer(Fixed(axis))
	/**
	  * @param cap Margin to place at each end of this stack
	  * @return Copy of this factory with the specified cap
	  */
	def withCap(cap: StackLength) = withCapPointer(Fixed(cap))
	/**
	  * @param layout Layout to use on this stack
	  * @return Copy of this factory with the specified layout
	  */
	def withLayout(layout: StackLayout) = withLayoutPointer(Fixed(layout))
	/**
	  * A segment group in which all the created components will be placed. Used to align the
	  * components with those of other stacks.
	  * None if segmentation is not used.
	  * @param group New segment group to use.
	  *              A segment group in which all the created components will be placed.
	  *              Used to align the components with those of other stacks.
	  * @return Copy of this factory with the specified segment group
	  */
	def withSegmentGroup(group: SegmentGroup): Repr = withSegmentGroup(Some(group))
	
	def mapAxisPointer(f: Mutate[Changing[Axis2D]]) = withAxisPointer(f(axisPointer))
	def mapCapPointer(f: Mutate[Changing[StackLength]]) = withCapPointer(f(capPointer))
	def mapLayoutPointer(f: Mutate[Changing[StackLayout]]) = withLayoutPointer(f(layoutPointer))
}

object ViewStackSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing view stacks
  * @param customDrawers Custom drawers to assign to created components
  * @param axisPointer   A pointer that determines the axis along which the items in the stacks
  *                      are placed.
  *                      Y yields columns and X yields rows.
  * @param layoutPointer A pointer that determines how the components are sized perpendicular to
  *                      the stack axis.
  *                      E.g. for columns, defines horizontal component placement and width.
  * @param capPointer    A pointer that specifies the margin placed at each end of the created
  *                      stacks
  * @param segmentGroup  A segment group in which all the created components will be placed. Used
  *                      to align the components with those of other stacks.
  *                      None if segmentation is not used.
  * @author Mikko Hilpinen
  * @since 12.01.2025, v1.5
  */
case class ViewStackSettings(customDrawers: Seq[CustomDrawer] = Empty, axisPointer: Changing[Axis2D] = Fixed(Axis.Y),
                             layoutPointer: Changing[StackLayout] = Fixed(StackLayout.Fit),
                             capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
                             segmentGroup: Option[SegmentGroup] = None)
	extends ViewStackSettingsLike[ViewStackSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withAxisPointer(p: Changing[Axis2D]) = copy(axisPointer = p)
	override def withCapPointer(p: Changing[StackLength]) = copy(capPointer = p)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
	override def withLayoutPointer(p: Changing[StackLayout]) = copy(layoutPointer = p)
	override def withSegmentGroup(group: Option[SegmentGroup]) = copy(segmentGroup = group)
}

/**
  * Common trait for factories that wrap a view stack settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 12.01.2025, v1.5
  */
trait ViewStackSettingsWrapper[+Repr] extends ViewStackSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ViewStackSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ViewStackSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def axisPointer = settings.axisPointer
	override def capPointer = settings.capPointer
	override def customDrawers = settings.customDrawers
	override def layoutPointer = settings.layoutPointer
	override def segmentGroup = settings.segmentGroup
	
	override def withAxisPointer(p: Changing[Axis2D]) = mapSettings { _.withAxisPointer(p) }
	override def withCapPointer(p: Changing[StackLength]) = mapSettings { _.withCapPointer(p) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = mapSettings { _.withCustomDrawers(drawers) }
	override def withLayoutPointer(p: Changing[StackLayout]) = mapSettings { _.withLayoutPointer(p) }
	override def withSegmentGroup(group: Option[SegmentGroup]) = mapSettings { _.withSegmentGroup(group) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ViewStackSettings => ViewStackSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing view stacks
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 12.01.2025, v1.5
  */
trait ViewStackFactoryLike[+Repr]
	extends ViewStackSettingsWrapper[Repr] with ViewContainerFactory[Stack, ReachComponentLike]
		with PartOfComponentHierarchy
{
	// ABSTRACT	--------------------
	
	protected def marginPointer: Changing[StackLength]
	
	/**
	  * @param p Pointer that contains the margin to place between items inside this stack
	  * @return A copy of this factory that uses the specified pointer
	  */
	def withMarginPointer(p: Changing[StackLength]): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * Copy of this factory that doesn't allow for any stack margins
	  */
	def withoutMargin = withMargin(StackLength.fixedZero)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[C <: ReachComponentLike, R](content: SwitchableOpenComponents[C, R]): SwitchableComponentsWrapResult[Stack, C, R] =
	{
		// Creates either a static stack or a view stack, based on whether the pointers are actually used
		// Case: All parameters are fixed values => Creates an immutable stack
		if (content.isEmpty || (axisPointer.isFixed && layoutPointer.isFixed && content.forall { _.result.isFixed })) {
			val stack = fixed(content, content.filter { _.result.value })
			stack.mapChild { _.map { c => c -> AlwaysTrue } }.withResult(content.result)
		}
		// Case: Values include changing values => Creates a view stack
		else {
			// May use segmentation
			segmentGroup match {
				// Case: Segmentation used
				case Some(group) =>
					// WET WET
					// Wraps the components into segments before placing them in this stack
					val wrappers = Open
						.many { hierarchies => group.wrap(content) { hierarchies.next() }.map { _.parentAndResult } }
						.component
					val stack = fromVisiblePointers(wrappers)
					// Still returns the components as the children and not the wrappers
					ComponentWrapResult(stack, content.map { _.componentAndResult }, content.result)
				
				// Case: No segmentation used
				case None =>
					val stack = fromVisiblePointers(content)
					ComponentWrapResult(stack, content.map { _.componentAndResult }, content.result)
			}
		}
	}
	
	override def pointer(content: Changing[SeparateOpenComponents[ReachComponentLike, _]]): Stack =
	{
		content.fixedValue match {
			// Case: Displayed content doesn't change
			case Some(staticContent) =>
				// Case: Layout doesn't change either => Creates a regular stack
				if (axisPointer.isFixed && layoutPointer.isFixed) {
					val stack = fixed(staticContent, staticContent).parent
					stack
				}
				// Case: Layout changes, however => Creates a simplified ViewStack
				else
					segmentGroup match {
						// Case: Segmentation is applied
						case Some(segmentGroup) =>
							// Wraps the components in segments
							val wrapped = Open { hierarchy =>
								val wrapResult = segmentGroup.wrapUnderSingle(hierarchy, staticContent)
								wrapResult.map { _.parent }
							}
							// The specified group defines the direction of this stack
							val stack = new ViewStack(parentHierarchy, Fixed(wrapped),
								settings.withAxis(segmentGroup.rowDirection), marginPointer)
								
							stack
							
						// Case: No segmentation is applied
						case None =>
							val stack = new ViewStack(parentHierarchy, Fixed(staticContent.map { _.component }), settings,
								marginPointer)
							
							// Attaches the components to this new stack using a single hierarchy block
							val mergedContent = merge(staticContent)
							mergedContent.attachTo(stack)
							
							stack
					}
					
			// Case: Displayed content changes
			case None =>
				// Creates the stack
				val componentsP = content.map { _.map { _.component } }
				val stack = new ViewStack(parentHierarchy, componentsP, settings, marginPointer)
				
				// Adds attachment management
				// Tracks visible components as hashcodes
				val visibleHashesP = componentsP.map { _.view.map { _.hashCode() }.toSet }
				// For each encountered component (hashcode), creates a new link pointer
				val attachmentPointers =
					Cache[Int, Flag] { componentHash => visibleHashesP.map { _.contains(componentHash) } }
				
				// When new components are introduced, makes sure they get attached to this stack
				content.addListenerWhile(parentHierarchy.linkPointer) { change =>
					change.newValue.foreach { open =>
						val hash = open.component.hashCode()
						// Case: Not previously attached => Creates a new link pointer and attaches the component
						if (!attachmentPointers.isValueCached(hash))
							open.attachTo(stack, attachmentPointers(hash))
					}
				}
				
				stack
		}
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param margin Margins to place between each item in this stack
	  * @return Copy of this factory with the specified margin
	  */
	def withMargin(margin: StackLength) = withMarginPointer(Fixed(margin))
	/**
	  * @param f A mapping function applied to stack margins
	  * @return Copy of this factory with mapped margin pointer
	  */
	def mapMargin(f: StackLength => StackLength) = withMarginPointer(marginPointer.map(f))
	
	private def fromVisiblePointers[C <: ReachComponentLike, R](content: SwitchableOpenComponents[C, R]) = {
		// Adds visible components -tracking
		val components = content.map { _.componentAndResult }
		val visibleContentP =
			CopyOnDemand { components.view.filter { _._2.value }.map { _._1 }.toOptimizedSeq }(
				ComponentCreationDefaults.componentLogger)
		
		components.foreach { case (_, visibleFlag) =>
			visibleFlag.addListenerWhile(parentHierarchy.linkPointer) { _ => visibleContentP.update() }
		}
		
		val stack = new ViewStack(parentHierarchy, visibleContentP, settings, marginPointer)
		content.foreach { open => open.attachTo(stack, open.result) }
		
		stack
	}
	
	/**
	  * Creates a fixed stack with this factory's settings.
	  * Assumes that content, axis and layout are all static.
	  * @param fullContent All content, including invisible items (called if segmentation is applied)
	  * @param visibleContent Visible content (called if segmentation is not applied)
	  * @return Component wrap result of the created stack
	  */
	private def fixed[C <: ReachComponentLike](fullContent: => SeparateOpenComponents[C, _],
	                                           visibleContent: => SeparateOpenComponents[C, _]) =
	{
		val fixedSettings = StackSettings(axis = axisPointer.value, layout = layoutPointer.value,
			capPointer = capPointer, customDrawers = customDrawers)
		val stackF = Stack(parentHierarchy).withSettings(fixedSettings).withMarginPointer(marginPointer)
		// Uses segmentation if available
		segmentGroup match {
			// Case: Segmentation used
			case Some(group) => stackF.segmented(fullContent, group)
			// Case: No segmentation used
			//       => Merges the content under a single OpenComponent & ComponentHierarchy instance
			case None => stackF(merge(visibleContent))
		}
	}
	/**
	  * Merges a number of separate open components under a single component hierarchy.
	  * This may be used for components which are displayed consistently.
	  * @param components Separated components in open format
	  * @tparam C Type of the components
	  * @return A combination of the specified components under a single component hierarchy
	  */
	private def merge[C](components: SeparateOpenComponents[C, _]): OpenComponent[Seq[C], Unit] = {
		NotEmpty(components) match {
			case Some(content) =>
				val commonHierarchy = content.head.hierarchy
				content.tail.foreach { _.hierarchy.replaceWith(commonHierarchy) }
				new OpenComponent(ComponentCreationResult(content.map { _.component }), commonHierarchy)
			
			case None =>
				new OpenComponent(ComponentCreationResult(Empty: Seq[C]), new SeedHierarchyBlock(parentHierarchy.top))
		}
	}
}

/**
  * Factory class that is used for constructing view stacks without using contextual information
  * @param marginPointer A pointer that determines the amount of empty space between adjacent
  *                      items in the created stacks
  * @author Mikko Hilpinen
  * @since 12.01.2025, v1.5
  */
case class ViewStackFactory(parentHierarchy: ComponentHierarchy, settings: ViewStackSettings = ViewStackSettings.default,
                            marginPointer: Changing[StackLength] = Fixed(StackLength.any))
	extends ViewStackFactoryLike[ViewStackFactory]
		with FromGenericContextFactory[BaseContextPropsView, ContextualViewStackFactory]
		with NonContextualViewContainerFactory[Stack, ReachComponentLike]
{
	// IMPLEMENTED	--------------------
	
	override def withContext[N <: BaseContextPropsView](context: N) =
		ContextualViewStackFactory(parentHierarchy, context, settings,
			customMarginPointer = {
				// Case: Using the default margin pointer => Won't forward it
				if (marginPointer.fixedValue.contains(StackLength.any))
					None
				// Case: Using a customized margin pointer => Specifies it as the custom margin pointer
				else
					Some(Right(marginPointer))
			})
	/**
	  * @param p A pointer that determines the amount of empty space between adjacent items in the
	  *          created stacks
	  * @return Copy of this factory with the specified margin pointer
	  */
	override def withMarginPointer(p: Changing[StackLength]): ViewStackFactory = copy(marginPointer = p)
	override def withSettings(settings: ViewStackSettings) = copy(settings = settings)
}

/**
  * Factory class used for constructing view stacks using contextual component creation
  * information
  * @param relatedFlag A pointer flag that signals whether the items in the created stacks should
  *                    be considered closely related to each other, resulting in a smaller margin
  *                    placed between them.
  * @tparam N Type of context used and passed along by this factory
  * @author Mikko Hilpinen
  * @since 12.01.2025, v1.5
  */
case class ContextualViewStackFactory[+N <: BaseContextPropsView](parentHierarchy: ComponentHierarchy, context: N,
                                                                  settings: ViewStackSettings = ViewStackSettings.default,
                                                                  customMarginPointer: Option[Either[Changing[SizeCategory], Changing[StackLength]]] = None,
                                                                  relatedFlag: Flag = AlwaysFalse)
	extends ViewStackFactoryLike[ContextualViewStackFactory[N]]
		with ContextualViewContainerFactory[N, BaseContextPropsView, Stack, ReachComponentLike, ContextualViewStackFactory]
{
	// ATTRIBUTES   ----------------
	
	override protected lazy val marginPointer: Changing[StackLength] = {
		customMarginPointer match {
			case Some(Left(sizePointer)) => context.scaledStackMarginPointer(sizePointer)
			case Some(Right(pointer)) => pointer
			case None => context.stackMarginPointerFor(relatedFlag)
		}
	}
	
	
	// COMPUTED	--------------------
	
	/**
	  * Copy of this factory that places the items close to each other
	  */
	def related = withRelatedFlag(AlwaysTrue)
	
	
	// IMPLEMENTED	--------------------
	
	override def withContext[N2 <: BaseContextPropsView](context: N2) = copy(context = context)
	override def withSettings(settings: ViewStackSettings) = copy(settings = settings)
	override def withMarginPointer(p: Changing[StackLength]): ContextualViewStackFactory[N] =
		copy(customMarginPointer = Some(Right(p)))
	
	
	// OTHER	--------------------
	
	/**
	  * @param cap Cap to place at each end of this stack
	  * @return Copy of this factory with that cap margin
	  */
	def withCap(cap: SizeCategory) = withCapSizePointer(Fixed(cap))
	/**
	  * @param p A pointer for stack cap sizes (general)
	  * @return Copy of this factory with that pointer in use
	  */
	def withCapSizePointer(p: Changing[SizeCategory]) =
		withCapPointer(context.scaledStackMarginPointer(p))
	
	/**
	  * @param margin Margin to place between items (general)
	  * @return Copy of this factory with those margins
	  */
	def withMargin(margin: SizeCategory) = withMarginSizePointer(Fixed(margin))
	/**
	  * @param p A pointer for margin sizes (general)
	  * @return Copy of this factory with that pointer in use
	  */
	def withMarginSizePointer(p: Changing[SizeCategory]) = copy(customMarginPointer = Some(Left(p)))
	
	/**
	  * @param relatedFlag A pointer flag that signals whether the items in the created stacks should
	  *                    be considered closely related to each other, resulting in a smaller margin
	  *                    placed between them.
	  * @return Copy of this factory with the specified related flag
	  */
	def withRelatedFlag(relatedFlag: Flag) = copy(relatedFlag = relatedFlag)
}

/**
  * Used for defining view stack creation settings outside the component building process
  * @author Mikko Hilpinen
  * @since 12.01.2025, v1.5
  */
case class ViewStackSetup(settings: ViewStackSettings = ViewStackSettings.default)
	extends ViewStackSettingsWrapper[ViewStackSetup] with ComponentFactoryFactory[ViewStackFactory]
		with FromGenericContextComponentFactoryFactory[BaseContextPropsView, ContextualViewStackFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = ViewStackFactory(hierarchy, settings)
	
	override def withContext[N <: BaseContextPropsView](hierarchy: ComponentHierarchy, context: N) =
		ContextualViewStackFactory(hierarchy, context, settings)
	
	override def withSettings(settings: ViewStackSettings) = copy(settings = settings)
}

object ViewStack extends ViewStackSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ViewStackSettings) = withSettings(settings)
}
/**
  * A pointer-based stack that adds and removes items based on activation pointer events
  * @author Mikko Hilpinen
  * @since 14.11.2020, v0.1
  */
class ViewStack(override val parentHierarchy: ComponentHierarchy, componentsP: Changing[Seq[ReachComponentLike]],
                 settings: ViewStackSettings, marginPointer: Changing[StackLength] = Fixed(StackLength.any))
	extends Stack
{
	// ATTRIBUTES	-------------------------------
	
	private val revalidateOnChange = ChangeListener.onAnyChange { revalidate() }
	
	/**
	  * A pointer to this stack's visibility state.
	  * This stack is visible while there is one or more components visible inside.
	  */
	override lazy val visibilityPointer = componentsP.map { _.nonEmpty }
	
	
	// INITIAL CODE	-------------------------------
	
	// When displayed components change, revalidates
	componentsP.addListener { change =>
		// Also resets the stack sizes of added components
		change.added.foreach { _.resetEveryCachedStackSize() }
		revalidate()
	}
	
	// Revalidates this component on other layout changes
	settings.axisPointer.addListenerWhile(parentHierarchy.linkPointer)(revalidateOnChange)
	settings.layoutPointer.addListenerWhile(parentHierarchy.linkPointer)(revalidateOnChange)
	marginPointer.addListenerWhile(parentHierarchy.linkPointer)(revalidateOnChange)
	settings.capPointer.addListenerWhile(parentHierarchy.linkPointer)(revalidateOnChange)
	
	
	// IMPLEMENTED	-------------------------------
	
	override def direction = settings.axisPointer.value
	override def layout = settings.layoutPointer.value
	override def margin = marginPointer.value
	override def cap = settings.capPointer.value
	override def customDrawers: Seq[CustomDrawer] = settings.customDrawers
	
	override def components = componentsP.value
}
