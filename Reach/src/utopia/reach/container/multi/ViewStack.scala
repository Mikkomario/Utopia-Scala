package utopia.reach.container.multi

import utopia.firmament.component.container.many.StackLike
import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.StackLength
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.DetachmentChoice
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.FlagLike.wrap
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.{ComponentFactoryFactory, FromGenericContextFactory, GenericContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.ComponentCreationResult.SwitchableCreations
import utopia.reach.component.wrapper.{ComponentWrapResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas2

object ViewStack extends Cff[ViewStackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewStackFactory(hierarchy)
}

case class ViewStackFactory(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[BaseContext, ContextualViewStackFactory]
{
	// COMPUTED	----------------------------------
	
	private implicit def canvas: ReachCanvas2 = parentHierarchy.top
	
	
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: BaseContext](context: N) =
		ContextualViewStackFactory(this, context)
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param contentFactory A stack content factory factory
	  * @tparam F Type of the used content factory
	  * @return A new view stack builder
	  */
	def builder[F](contentFactory: ComponentFactoryFactory[F]) = new ViewStackBuilder[F](this, contentFactory)
	
	/**
	  * Creates a new stack
	  * @param content Content placed in the stack. Each component needs to have an attachment pointer
	  *                as a creation result
	  * @param directionPointer A pointer determining the direction of this stack (default = always vertical (Y))
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = always any, preferring 0)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike](content: Vector[OpenComponent[C, Changing[Boolean]]],
	                                   directionPointer: Changing[Axis2D] = Fixed(Y),
	                                   layoutPointer: Changing[StackLayout] = Fixed(Fit),
	                                   marginPointer: Changing[StackLength] = Fixed(StackLength.any),
	                                   capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	                                   customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val stack = new ViewStack[C](parentHierarchy, content.map { open => open.component -> open.result },
			directionPointer, layoutPointer, marginPointer, capPointer, customDrawers)
		content.foreach { open => open.attachTo(stack, open.result) }
		ComponentWrapResult(stack, content.map { _.component })
	}
	
	/**
	  * Creates a new stack with immutable style
	  * @param content Content placed in the stack. Each component needs to have an attachment pointer
	  *                as a creation result
	  * @param direction the direction of this stack (default = vertical = Y)
	  * @param layout this stack's layout (default = Fit)
	  * @param margin the margin between the items in this stack (default = any, preferring 0)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withFixedStyle[C <: ReachComponentLike](content: Vector[OpenComponent[C, Changing[Boolean]]],
	                                            direction: Axis2D = Y, layout: StackLayout = Fit,
	                                            margin: StackLength = StackLength.any,
	                                            cap: StackLength = StackLength.fixedZero,
	                                            customDrawers: Vector[CustomDrawer] = Vector()) =
		apply[C](content, Fixed(direction), Fixed(layout), Fixed(margin), Fixed(cap),
			customDrawers)
	
	/**
	  * Creates a new stack with content aligned with that of some other container
	  * @param group A segmented group that defines content alignment
	  * @param content Content to place on this stack. Each component is paired with a pointer that
	  *                determines whether it should be connected to this stack.
	  *                None is considered to always be connected.
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = always any, preferring 0)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @return A new stack
	  */
	def segmented(group: SegmentGroup, content: Seq[OpenComponent[ReachComponentLike, Changing[Boolean]]],
	              layoutPointer: Changing[StackLayout] = Fixed(Fit),
	              marginPointer: Changing[StackLength] = Fixed(StackLength.any),
	              capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	              customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val wrappers = Open.many { hierarchies =>
			group.wrap(content) { hierarchies.next() }.map { _.parentAndResult }
		}.component
		apply(wrappers, Fixed(group.rowDirection), layoutPointer, marginPointer, capPointer, customDrawers)
	}
	
	/**
	  * Creates a new stack with content aligned with that of some other container
	  * @param group A segmented group that defines content alignment
	  * @param content Content to place on this stack. Each component is paired with an optional pointer that
	  *                determines whether it should be connected to this stack.
	  *                None is considered to always be connected.
	  * @param layout this stack's layout (default = Fit)
	  * @param margin the margin between the items in this stack (default = any, preferring 0)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @return A new stack
	  */
	def segmentedWithFixedStyle(group: SegmentGroup,
	                            content: Vector[OpenComponent[ReachComponentLike, Changing[Boolean]]],
	                            layout: StackLayout = Fit, margin: StackLength = StackLength.any,
	                            cap: StackLength = StackLength.fixedZero,
	                            customDrawers: Vector[CustomDrawer] = Vector()) =
		segmented(group, content, Fixed(layout), Fixed(margin), Fixed(cap), customDrawers)
}

case class ContextualViewStackFactory[N <: BaseContext](stackFactory: ViewStackFactory, context: N)
	extends GenericContextualFactory[N, BaseContext, ContextualViewStackFactory]
{
	// COMPUTED	------------------------------------
	
	private implicit def canvas: ReachCanvas2 = stackFactory.parentHierarchy.top
	
	/**
	  * @return A version of this factory which doesn't utilize component creation context
	  */
	def withoutContext = stackFactory
	
	
	// IMPLEMENTED	--------------------------------
	
	override def withContext[N2 <: BaseContext](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * @param contentFactory A factory for producing component creation factories
	  * @tparam F Type of component creation factories used
	  * @return A new view stack builder
	  */
	def build[F](contentFactory: Ccff[N, F]) = new ContextualViewStackBuilder[N, F](this, contentFactory)
	
	/**
	  * Creates a new stack
	  * @param content Content placed in the stack. Each component needs to have an attachment pointer
	  *                as a creation result
	  * @param directionPointer A pointer determining the direction of this stack (default = always vertical (Y))
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = determined by context)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike](content: Vector[OpenComponent[C, Changing[Boolean]]],
	                                   directionPointer: Changing[Axis2D] = Fixed(Y),
	                                   layoutPointer: Changing[StackLayout] = Fixed(Fit),
	                                   marginPointer: Changing[StackLength] = Fixed(context.stackMargin),
	                                   capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	                                   customDrawers: Vector[CustomDrawer] = Vector()) =
		stackFactory[C](content, directionPointer, layoutPointer, marginPointer, capPointer, customDrawers)
	
	/**
	  * Creates a new stack with immutable style, yet changing direction
	  * @param content Content placed in the stack. Each component needs to have an optional attachment pointer
	  *                as a creation result
	  * @param directionPointer A pointer to the direction of this stack
	  * @param layout this stack's layout (default = Fit)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param areRelated Whether the items in this stack should be considered closely related (affects margin used)
	  *                   (default = false)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withChangingDirection[C <: ReachComponentLike](content: Vector[OpenComponent[C, Changing[Boolean]]],
	                                                   directionPointer: Changing[Axis2D], layout: StackLayout = Fit,
	                                                   cap: StackLength = StackLength.fixedZero,
	                                                   customDrawers: Vector[CustomDrawer] = Vector(),
	                                                   areRelated: Boolean = false) =
		stackFactory[C](content, directionPointer, Fixed(layout),
			Fixed(if (areRelated) context.smallStackMargin else context.stackMargin),
			Fixed(cap), customDrawers)
	
	/**
	  * Creates a new stack with immutable style
	  * @param content Content placed in the stack. Each component needs to have an optional attachment pointer
	  *                as a creation result
	  * @param direction the direction of this stack (default = vertical = Y)
	  * @param layout this stack's layout (default = Fit)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param areRelated Whether the items in this stack should be considered closely related (affects margin used)
	  *                   (default = false)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withFixedStyle[C <: ReachComponentLike](content: Vector[OpenComponent[C, Changing[Boolean]]],
	                                            direction: Axis2D = Y, layout: StackLayout = Fit,
	                                            cap: StackLength = StackLength.fixedZero,
	                                            customDrawers: Vector[CustomDrawer] = Vector(),
	                                            areRelated: Boolean = false) =
		withChangingDirection[C](content, Fixed(direction), layout, cap, customDrawers, areRelated)
	
	/**
	  * Creates a new stack with immutable style and no margin between items
	  * @param content Content placed in the stack. Each component needs to have an optional attachment pointer
	  *                as a creation result
	  * @param direction the direction of this stack (default = vertical = Y)
	  * @param layout this stack's layout (default = Fit)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withoutMargin[C <: ReachComponentLike](content: Vector[OpenComponent[C, Changing[Boolean]]],
	                                           direction: Axis2D = Y, layout: StackLayout = Fit,
	                                           cap: StackLength = StackLength.fixedZero,
	                                           customDrawers: Vector[CustomDrawer] = Vector()) =
		stackFactory[C](content, Fixed(direction), Fixed(layout), Fixed(StackLength.fixedZero),
			Fixed(cap), customDrawers)
	
	/**
	  * Creates a new stack with content aligned with that of some other container
	  * @param group A segmented group that defines content alignment
	  * @param content Content to place on this stack. Each component is paired with an optional pointer that
	  *                determines whether it should be connected to this stack.
	  *                None is considered to always be connected.
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = determined by context)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @return A new stack
	  */
	def segmented(group: SegmentGroup, content: Seq[OpenComponent[ReachComponentLike, Changing[Boolean]]],
	              layoutPointer: Changing[StackLayout] = Fixed(Fit),
	              marginPointer: Changing[StackLength] = Fixed(context.stackMargin),
	              capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	              customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val wrappers = Open.many { hierarchies =>
			group.wrap(content) { hierarchies.next() }.map { _.parentAndResult }
		}.component
		apply(wrappers, Fixed(group.rowDirection), layoutPointer, marginPointer, capPointer, customDrawers)
	}
	
	/**
	  * Creates a new stack with content aligned with that of some other container
	  * @param group A segmented group that defines content alignment
	  * @param content Content to place on this stack. Each component is paired with an optional pointer that
	  *                determines whether it should be connected to this stack.
	  *                None is considered to always be connected.
	  * @param layout this stack's layout (default = Fit)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param areRelated Whether the items in this stack should be considered closely related (affects margin used)
	  *                   (default = false)
	  * @return A new stack
	  */
	def segmentedWithFixedStyle(group: SegmentGroup,
	                            content: Vector[OpenComponent[ReachComponentLike, Changing[Boolean]]],
	                            layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
	                            customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false) =
		stackFactory.segmentedWithFixedStyle(group, content, layout,
			if (areRelated) context.stackMargin else context.smallStackMargin, cap, customDrawers)
}

class ViewStackBuilder[+F](factory: ViewStackFactory, contentFactory: ComponentFactoryFactory[F])
{
	// IMPLICIT	---------------------------------
	
	private implicit def canvas: ReachCanvas2 = factory.parentHierarchy.top
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new stack
	  * @param directionPointer A pointer determining the direction of this stack (default = always vertical (Y))
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = always any, preferring 0)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike, R](directionPointer: Changing[Axis2D] = Fixed(Y),
	                                      layoutPointer: Changing[StackLayout] = Fixed(Fit),
	                                      marginPointer: Changing[StackLength] = Fixed(StackLength.any),
	                                      capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	                                      customDrawers: Vector[CustomDrawer] = Vector())
									  (fill: Iterator[F] => SwitchableCreations[C, R]) =
	{
		val content = Open.manyUsing(contentFactory)(fill)
		factory(content.component, directionPointer, layoutPointer, marginPointer, capPointer, customDrawers)
			.withResult(content.result)
	}
	
	/**
	  * Creates a new stack with immutable style
	  * @param direction the direction of this stack (default = vertical = Y)
	  * @param layout this stack's layout (default = Fit)
	  * @param margin the margin between the items in this stack (default = any, preferring 0)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withFixedStyle[C <: ReachComponentLike, R](direction: Axis2D = Y, layout: StackLayout = Fit,
												margin: StackLength = StackLength.any,
												cap: StackLength = StackLength.fixedZero,
												customDrawers: Vector[CustomDrawer] = Vector())
												  (fill: Iterator[F] => SwitchableCreations[C, R]) =
		apply[C, R](Fixed(direction), Fixed(layout), Fixed(margin), Fixed(cap),
			customDrawers)(fill)
	
	/**
	  * Builds a new segmented stack
	  * @param group A segmented group that defines content alignment
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = always any, preferring 0)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam R Type of additional creation result
	  * @return A new stack
	  */
	def segmented[R](group: SegmentGroup, layoutPointer: Changing[StackLayout] = Fixed(Fit),
	                 marginPointer: Changing[StackLength] = Fixed(StackLength.any),
	                 capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	                 customDrawers: Vector[CustomDrawer] = Vector())
					(fill: Iterator[F] => SwitchableCreations[ReachComponentLike, R]) =
	{
		val content = Open.manyUsing(contentFactory)(fill)
		factory.segmented(group, content.component, layoutPointer, marginPointer, capPointer, customDrawers)
			.withResult(content.result)
	}
	
	/**
	  * Builds a new segmented stack
	  * @param group A segmented group that defines content alignment
	  * @param layout this stack's layout (default = Fit)
	  * @param margin the margin between the items in this stack (default = any, preferring 0)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam R Type of additional creation result
	  * @return A new stack
	  */
	def segmentedWithFixedStyle[R](group: SegmentGroup, layout: StackLayout = Fit,
								   margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixedZero,
								   customDrawers: Vector[CustomDrawer] = Vector())
								  (fill: Iterator[F] => SwitchableCreations[ReachComponentLike, R]) =
		segmented(group, Fixed(layout), Fixed(margin), Fixed(cap), customDrawers)(fill)
}

class ContextualViewStackBuilder[N <: BaseContext, +F](stackFactory: ContextualViewStackFactory[N],
                                                       contentFactory: Ccff[N, F])
{
	// IMPLICIT	---------------------------------
	
	implicit def canvas: ReachCanvas2 = stackFactory.stackFactory.parentHierarchy.top
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return The component creation context used by this builder
	  */
	def context = stackFactory.context
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new stack
	  * @param directionPointer A pointer determining the direction of this stack (default = always vertical (Y))
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = determined by context)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike, R](directionPointer: Changing[Axis2D] = Fixed(Y),
	                                      layoutPointer: Changing[StackLayout] = Fixed(Fit),
	                                      marginPointer: Changing[StackLength] = Fixed(context.stackMargin),
	                                      capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	                                      customDrawers: Vector[CustomDrawer] = Vector())
									  (fill: Iterator[F] => SwitchableCreations[C, R]) =
	{
		val content = Open.withContext(stackFactory.context).many(contentFactory)(fill)
		stackFactory(content.component, directionPointer, layoutPointer, marginPointer, capPointer, customDrawers)
			.withResult(content.result)
	}
	
	/**
	  * Creates a new stack with static layout yet changing direction
	  * @param directionPointer A pointer determining the direction of this stack
	  * @param layout this stack's layout (default = Fit)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param areRelated Whether the items in this stack should be considered closely related (affects margin used)
	  *                   (default = false)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withChangingDirection[C <: ReachComponentLike, R](directionPointer: Changing[Axis2D], layout: StackLayout = Fit,
	                                                      cap: StackLength = StackLength.fixedZero,
	                                                      customDrawers: Vector[CustomDrawer] = Vector(),
	                                                      areRelated: Boolean = false)
													  (fill: Iterator[F] => SwitchableCreations[C, R]) =
		apply[C, R](directionPointer, Fixed(layout),
			Fixed(if (areRelated) context.stackMargin else context.smallStackMargin),
			Fixed(cap), customDrawers)(fill)
	
	/**
	  * Creates a new stack with static layout
	  * @param direction The direction of this stack (default = vertical = Y)
	  * @param layout this stack's layout (default = Fit)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param areRelated Whether the items in this stack should be considered closely related (affects margin used)
	  *                   (default = false)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withFixedStyle[C <: ReachComponentLike, R](direction: Axis2D = Y, layout: StackLayout = Fit,
												cap: StackLength = StackLength.fixedZero,
												customDrawers: Vector[CustomDrawer] = Vector(),
												areRelated: Boolean = false)
											   (fill: Iterator[F] => SwitchableCreations[C, R]) =
		withChangingDirection[C, R](Fixed(direction), layout, cap, customDrawers, areRelated)(fill)
	
	/**
	  * Builds a new segmented stack
	  * @param group A segmented group that defines content alignment
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = always any, preferring 0)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam R Type of additional creation result
	  * @return A new stack
	  */
	def segmented[R](group: SegmentGroup, layoutPointer: Changing[StackLayout] = Fixed(Fit),
	                 marginPointer: Changing[StackLength] = Fixed(context.stackMargin),
	                 capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
	                 customDrawers: Vector[CustomDrawer] = Vector())
					(fill: Iterator[F] => SwitchableCreations[ReachComponentLike, R]) =
	{
		val content = Open.withContext(context).many(contentFactory)(fill)
		stackFactory.segmented(group, content.component, layoutPointer, marginPointer, capPointer, customDrawers)
			.withResult(content.result)
	}
	
	/**
	  * Builds a new segmented stack
	  * @param group A segmented group that defines content alignment
	  * @param layout this stack's layout (default = Fit)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @param areRelated Whether the items in this stack should be considered closely related (affects margin used)
	  *                   (default = false)
	  * @param fill A function for producing the contents inside this stack. Accepts an infinite iterator that produces
	  *             a component factory for each component separately. Returns possibly multiple components, with their
	  *             individual connection pointers (if defined). The number of returned components should match exactly
	  *             the number of calls to the passed iterator's next(). Sharing a component hierarchy or a factory
	  *             between multiple components is not allowed.
	  * @tparam R Type of additional creation result
	  * @return A new stack
	  */
	def segmentedWithFixedStyle[R](group: SegmentGroup, layout: StackLayout = Fit,
								   cap: StackLength = StackLength.fixedZero,
								   customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
								  (fill: Iterator[F] => SwitchableCreations[ReachComponentLike, R]) =
		segmented(group, Fixed(layout),
			Fixed(if (areRelated) context.stackMargin else context.smallStackMargin),
			Fixed(cap), customDrawers)(fill)
}

/**
  * A pointer-based stack that adds and removes items based on activation pointer events
  * @author Mikko Hilpinen
  * @since 14.11.2020, v0.1
  */
class ViewStack[C <: ReachComponentLike](override val parentHierarchy: ComponentHierarchy,
                                         componentData: Vector[(C, Changing[Boolean])],
                                         directionPointer: Changing[Axis2D] = Fixed(Y),
                                         layoutPointer: Changing[StackLayout] = Fixed(Fit),
                                         marginPointer: Changing[StackLength] = Fixed(StackLength.any),
                                         capPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
                                         override val customDrawers: Vector[CustomDrawer] = Vector())
	extends CustomDrawReachComponent with StackLike[C]
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
	lazy val visibilityPointer = {
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
	
	override def children = components
}
