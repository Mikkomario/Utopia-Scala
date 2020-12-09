package utopia.reflection.container.reach

import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.flow.event.{ChangeListener, ChangingLike, Fixed}
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.factory.{ComponentFactoryFactory, ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.ComponentCreationResult.SwitchableCreations
import utopia.reflection.component.reach.wrapper.{ComponentWrapResult, Open, OpenComponent}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.stack.template.layout.StackLike2
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.stack.StackLength

object ViewStack extends ContextInsertableComponentFactoryFactory[BaseContextLike, ViewStackFactory,
	ContextualViewStackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewStackFactory(hierarchy)
}

case class ViewStackFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[BaseContextLike, ContextualViewStackFactory]
{
	// COMPUTED	----------------------------------
	
	private implicit def canvas: ReachCanvas = parentHierarchy.top
	
	
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: BaseContextLike](context: N) =
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
	  * @param content Content placed in the stack. Each component needs to have an optional attachment pointer
	  *                as a creation result
	  * @param directionPointer A pointer determining the direction of this stack (default = always vertical (Y))
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = always any, preferring 0)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike](content: Vector[OpenComponent[C, Option[ChangingLike[Boolean]]]],
									   directionPointer: ChangingLike[Axis2D] = Fixed(Y),
									   layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
									   marginPointer: ChangingLike[StackLength] = Fixed(StackLength.any),
									   capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
									   customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val stack = new ViewStack[C](parentHierarchy, content.map { open => open.component -> open.result },
			directionPointer, layoutPointer, marginPointer, capPointer, customDrawers)
		content.foreach { open => open.attachTo(stack, open.result) }
		ComponentWrapResult(stack, content.map { _.component })
	}
	
	/**
	  * Creates a new stack with immutable style
	  * @param content Content placed in the stack. Each component needs to have an optional attachment pointer
	  *                as a creation result
	  * @param direction the direction of this stack (default = vertical = Y)
	  * @param layout this stack's layout (default = Fit)
	  * @param margin the margin between the items in this stack (default = any, preferring 0)
	  * @param cap the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def withFixedStyle[C <: ReachComponentLike](content: Vector[OpenComponent[C, Option[ChangingLike[Boolean]]]],
												direction: Axis2D = Y, layout: StackLayout = Fit,
												margin: StackLength = StackLength.any,
												cap: StackLength = StackLength.fixedZero,
												customDrawers: Vector[CustomDrawer] = Vector()) =
		apply[C](content, Fixed(direction), Fixed(layout), Fixed(margin), Fixed(cap),
			customDrawers)
	
	/**
	  * Creates a new stack with content aligned with that of some other container
	  * @param group A segmented group that defines content alignment
	  * @param content Content to place on this stack. Each component is paired with an optional pointer that
	  *                determines whether it should be connected to this stack.
	  *                None is considered to always be connected.
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = always any, preferring 0)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @return A new stack
	  */
	def segmented(group: SegmentGroup, content: Seq[OpenComponent[ReachComponentLike, Option[ChangingLike[Boolean]]]],
				  layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
				  marginPointer: ChangingLike[StackLength] = Fixed(StackLength.any),
				  capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
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
								content: Vector[OpenComponent[ReachComponentLike, Option[ChangingLike[Boolean]]]],
								layout: StackLayout = Fit, margin: StackLength = StackLength.any,
								cap: StackLength = StackLength.fixedZero,
								customDrawers: Vector[CustomDrawer] = Vector()) =
		segmented(group, content, Fixed(layout), Fixed(margin), Fixed(cap), customDrawers)
}

case class ContextualViewStackFactory[N <: BaseContextLike](stackFactory: ViewStackFactory, context: N)
	extends ContextualComponentFactory[N, BaseContextLike, ContextualViewStackFactory]
{
	// COMPUTED	------------------------------------
	
	private implicit def canvas: ReachCanvas = stackFactory.parentHierarchy.top
	
	/**
	  * @return A version of this factory which doesn't utilize component creation context
	  */
	def withoutContext = stackFactory
	
	
	// IMPLEMENTED	--------------------------------
	
	override def withContext[N2 <: BaseContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * @param contentFactory A factory for producing component creation factories
	  * @tparam F Type of component creation factories used
	  * @return A new view stack builder
	  */
	def builder[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
	(contentFactory: ContextInsertableComponentFactoryFactory[_ >: N, _, F]) =
		new ContextualViewStackBuilder[N, F](this, contentFactory)
	
	/**
	  * Creates a new stack
	  * @param content Content placed in the stack. Each component needs to have an optional attachment pointer
	  *                as a creation result
	  * @param directionPointer A pointer determining the direction of this stack (default = always vertical (Y))
	  * @param layoutPointer A pointer to this stack's layout (default = always Fit)
	  * @param marginPointer A pointer to the margin between the items in this stack (default = determined by context)
	  * @param capPointer A pointer to the cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers applied to this stack (default = empty)
	  * @tparam C Type of components in this stack
	  * @return A new stack
	  */
	def apply[C <: ReachComponentLike](content: Vector[OpenComponent[C, Option[ChangingLike[Boolean]]]],
									   directionPointer: ChangingLike[Axis2D] = Fixed(Y),
									   layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
									   marginPointer: ChangingLike[StackLength] = Fixed(context.defaultStackMargin),
									   capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
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
	def withChangingDirection[C <: ReachComponentLike](content: Vector[OpenComponent[C, Option[ChangingLike[Boolean]]]],
													   directionPointer: ChangingLike[Axis2D], layout: StackLayout = Fit,
													   cap: StackLength = StackLength.fixedZero,
													   customDrawers: Vector[CustomDrawer] = Vector(),
													   areRelated: Boolean = false) =
		stackFactory[C](content, directionPointer, Fixed(layout),
			Fixed(if (areRelated) context.relatedItemsStackMargin else context.defaultStackMargin),
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
	def withFixedStyle[C <: ReachComponentLike](content: Vector[OpenComponent[C, Option[ChangingLike[Boolean]]]],
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
	def withoutMargin[C <: ReachComponentLike](content: Vector[OpenComponent[C, Option[ChangingLike[Boolean]]]],
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
	def segmented(group: SegmentGroup, content: Seq[OpenComponent[ReachComponentLike, Option[ChangingLike[Boolean]]]],
				  layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
				  marginPointer: ChangingLike[StackLength] = Fixed(context.defaultStackMargin),
				  capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
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
								content: Vector[OpenComponent[ReachComponentLike, Option[ChangingLike[Boolean]]]],
								layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
								customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false) =
		stackFactory.segmentedWithFixedStyle(group, content, layout,
			if (areRelated) context.defaultStackMargin else context.relatedItemsStackMargin, cap, customDrawers)
}

class ViewStackBuilder[+F](factory: ViewStackFactory, contentFactory: ComponentFactoryFactory[F])
{
	// IMPLICIT	---------------------------------
	
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	
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
	def apply[C <: ReachComponentLike, R](directionPointer: ChangingLike[Axis2D] = Fixed(Y),
										  layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
										  marginPointer: ChangingLike[StackLength] = Fixed(StackLength.any),
										  capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
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
	def segmented[R](group: SegmentGroup, layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
					 marginPointer: ChangingLike[StackLength] = Fixed(StackLength.any),
					 capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
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

class ContextualViewStackBuilder[N <: BaseContextLike, +F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
(stackFactory: ContextualViewStackFactory[N], contentFactory: ContextInsertableComponentFactoryFactory[_ >: N, _, F])
{
	// IMPLICIT	---------------------------------
	
	implicit def canvas: ReachCanvas = stackFactory.stackFactory.parentHierarchy.top
	
	
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
	def apply[C <: ReachComponentLike, R](directionPointer: ChangingLike[Axis2D] = Fixed(Y),
									   layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
									   marginPointer: ChangingLike[StackLength] = Fixed(context.defaultStackMargin),
									   capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
									   customDrawers: Vector[CustomDrawer] = Vector())
									  (fill: Iterator[F[N]] => SwitchableCreations[C, R]) =
	{
		val content = Open.manyWithContext(contentFactory, stackFactory.context)(fill)
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
	def withChangingDirection[C <: ReachComponentLike, R](directionPointer: ChangingLike[Axis2D], layout: StackLayout = Fit,
													   cap: StackLength = StackLength.fixedZero,
													   customDrawers: Vector[CustomDrawer] = Vector(),
													   areRelated: Boolean = false)
													  (fill: Iterator[F[N]] => SwitchableCreations[C, R]) =
		apply[C, R](directionPointer, Fixed(layout),
			Fixed(if (areRelated) context.defaultStackMargin else context.relatedItemsStackMargin),
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
											   (fill: Iterator[F[N]] => SwitchableCreations[C, R]) =
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
	def segmented[R](group: SegmentGroup, layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
					 marginPointer: ChangingLike[StackLength] = Fixed(context.defaultStackMargin),
					 capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
					 customDrawers: Vector[CustomDrawer] = Vector())
					(fill: Iterator[F[N]] => SwitchableCreations[ReachComponentLike, R]) =
	{
		val content = Open.manyWithContext(contentFactory, context)(fill)
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
								  (fill: Iterator[F[N]] => SwitchableCreations[ReachComponentLike, R]) =
		segmented(group, Fixed(layout),
			Fixed(if (areRelated) context.defaultStackMargin else context.relatedItemsStackMargin),
			Fixed(cap), customDrawers)(fill)
}

/**
  * A pointer-based stack that adds and removes items based on activation pointer events
  * @author Mikko Hilpinen
  * @since 14.11.2020, v2
  */
class ViewStack[C <: ReachComponentLike](override val parentHierarchy: ComponentHierarchy,
										 componentData: Vector[(C, Option[ChangingLike[Boolean]])],
										 directionPointer: ChangingLike[Axis2D] = Fixed(Y),
										 layoutPointer: ChangingLike[StackLayout] = Fixed(Fit),
										 marginPointer: ChangingLike[StackLength] = Fixed(StackLength.any),
										 capPointer: ChangingLike[StackLength] = Fixed(StackLength.fixedZero),
										 override val customDrawers: Vector[CustomDrawer] = Vector())
	extends CustomDrawReachComponent with StackLike2[C]
{
	// ATTRIBUTES	-------------------------------
	
	private val activeComponentsCache = ResettableLazy {
		componentData.flatMap { case (component, pointer) =>
			if (pointer.forall { _.value })
				Some(component)
			else
				None
		}
	}
	
	private val revalidateOnChange = ChangeListener.onAnyChange { revalidate() }
	private lazy val resetActiveComponentsOnChange = ChangeListener.onAnyChange {
		activeComponentsCache.reset()
		revalidate()
	}
	
	
	// INITIAL CODE	-------------------------------
	
	// Updates components list when component pointers get updated
	componentData.flatMap { _._2 }.foreach { _.addListener(resetActiveComponentsOnChange) }
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
