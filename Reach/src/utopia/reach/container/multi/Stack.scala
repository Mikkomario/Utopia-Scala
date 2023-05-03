package utopia.reach.container.multi

import utopia.firmament.component.container.many.StackLike
import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.{Center, Fit, Leading, Trailing}
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
import utopia.reach.component.factory.{ComponentFactoryFactory, FromGenericContextFactory, GenericContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentCreationResult, ComponentWrapResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas

/**
 * Common trait for all Reach stack implementations, regardless of implementation style
 * @tparam C Type of components held within this stack
 */
trait Stack[C <: ReachComponentLike] extends CustomDrawReachComponent with StackLike[C]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return A pointer that contains true while this stack should be displayed. I.e. is non-empty.
	 */
	def visibilityPointer: FlagLike
	
	
	// IMPLEMENTED  ----------------------
	
	override def children = components
}

object Stack extends Cff[StackFactory] with Gccff[BaseContext, ContextualStackFactory]
{
	// IMPLEMENTED  ----------------------
	
	override def apply(hierarchy: ComponentHierarchy) = StackFactory(hierarchy)
	
	override def withContext[N <: BaseContext](parentHierarchy: ComponentHierarchy, context: N): ContextualStackFactory[N] =
		apply(parentHierarchy).withContext(context)
	
	
	// OTHER    -------------------------
	
	/**
	 * Creates a new stack
	 * @param parentHierarchy Component hierarchy this stack will be attached to
	 * @param components Components to place within this stack
	 * @param direction Direction of this stack (default = Y = vertical)
	 * @param layout Layout of this stack (default = Fit)
	 * @param margin Margin placed between items (default = any)
	 * @param cap Cap placed at each end of this stack (default = 0)
	 * @param customDrawers Custom drawers to apply (default = empty)
	 * @tparam C Type of items within this stack
	 * @return A new stack
	 */
	def raw[C <: ReachComponentLike](parentHierarchy: ComponentHierarchy, components: Vector[C],
	                                   direction: Axis2D = Y, layout: StackLayout = Fit,
	                                   margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixedZero,
	                                   customDrawers: Vector[CustomDrawer] = Vector()): Stack[C] =
		new _Stack[C](parentHierarchy, components, direction, layout, margin, cap, customDrawers)
	
	
	// NESTED   -------------------------
	
	private class _Stack[C <: ReachComponentLike](override val parentHierarchy: ComponentHierarchy,
	                                              override val components: Vector[C], override val direction: Axis2D,
	                                              override val layout: StackLayout, override val margin: StackLength,
	                                              override val cap: StackLength,
	                                              override val customDrawers: Vector[CustomDrawer])
		extends Stack[C]
	{
		override lazy val visibilityPointer: FlagLike = if (components.isEmpty) AlwaysFalse else AlwaysTrue
	}
}

case class StackFactory(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[BaseContext, ContextualStackFactory]
{
	// COMPUTED	--------------------------------
	
	private implicit def canvas: ReachCanvas = parentHierarchy.top
	
	/**
	  * Creates a new stack builder
	  * @param contentFactory Factory used for stack content factories
	  * @tparam F Type of content factory
	  * @return A stack builder
	  */
	def build[F](contentFactory: ComponentFactoryFactory[F]) = new StackBuilder[F](this, contentFactory)
	
	
	// IMPLEMENTED	----------------------------
	
	override def withContext[N <: BaseContext](context: N) =
		ContextualStackFactory(this, context)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new stack of items
	  * @param content Content to attach to this stack
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param margin Margin placed between each component (default = any margin, preferring 0)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], direction: Axis2D = Y,
										   layout: StackLayout = Fit, margin: StackLength = StackLength.any,
										   cap: StackLength = StackLength.fixedZero,
										   customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val stack = Stack.raw[C](parentHierarchy, content.component, direction, layout, margin, cap, customDrawers)
		content attachTo stack
	}
	
	/**
	  * Creates a new stack of items with no margin between them
	  * @param content Content to attach to this stack
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def withoutMargin[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], direction: Axis2D = Y,
												  layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
												  customDrawers: Vector[CustomDrawer] = Vector()) =
		apply(content, direction, layout, StackLength.fixedZero, cap, customDrawers)
	
	/**
	  * Creates a new stack where each item belongs to a shared segment (so that they are aligned with another
	  * stack's / container's items)
	  * @param group A segment group that determines content alignment
	  * @param content Components to place in this stack
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param margin Margin placed between each component (default = any margin, preferring 0)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @tparam R Type of an individual component creation result
	  * @return A new stack
	  */
	def segmented[R](group: SegmentGroup, content: Seq[OpenComponent[ReachComponentLike, R]], layout: StackLayout = Fit,
					 margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixedZero,
					 customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		// Wraps the components in segments first
		val wrapped = Open { hierarchy =>
			val wrapResult = group.wrapUnderSingle(hierarchy, content)
			wrapResult.map { _.parent } -> wrapResult.map { _.result }
		}
		// Then wraps the segments in a stack
		apply(wrapped, group.rowDirection, layout, margin, cap, customDrawers)
	}
	
	/**
	  * Creates a new stack that contains two items
	  * @param content Items to place in this stack
	  * @param alignment Alignment to use when placing the items. The direction of the alignment determines the
	  *                  position of the <b>first</b> item in the <i>content</i> parameter. Eg. Left alignment means
	  *                  that the first item will be placed at the left side and the second item on the right.
	  *                  Bottom alignment means that the first item will be placed at the bottom and the second at
	  *                  the top.
	  * @param margin Margin placed between the items (default = any, preferring 0)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param forceFitLayout Whether layout should always be set to <i>Fit</i>, regardless of alignment
	  * @tparam C Type of the components
	  * @tparam R Type of additional creation result
	  * @return A new stack with the two items in it
	  */
	def forPair[C <: ReachComponentLike, R](content: OpenComponent[Pair[C], R], alignment: Alignment,
	                                        margin: StackLength = StackLength.any,
	                                        cap: StackLength = StackLength.fixedZero,
	                                        customDrawers: Vector[CustomDrawer] = Vector(),
	                                        forceFitLayout: Boolean = false): ComponentWrapResult[Stack[C], Vector[C], R] =
	{
		// Specifies stack axis, layout and item order based on the alignment
		// The image label always goes to the direction of the alignment
		// (Eg. Left = Image, then text (centered vertically), Bottom = Text (centered horizontally), then image)
		val (axis, sign, layout) = alignment.horizontalDirection match
		{
			case Some(horizontal) =>
				val layout = alignment.verticalDirection match
				{
					case Some(vertical) =>
						vertical match
						{
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
		apply(orderedContent, axis, if (forceFitLayout) Fit else layout, margin, cap, customDrawers)
	}
}

case class ContextualStackFactory[N <: BaseContext](stackFactory: StackFactory, context: N)
	extends GenericContextualFactory[N, BaseContext, ContextualStackFactory]
{
	// IMPLEMENTED	--------------------------------
	
	override def withContext[C2 <: BaseContext](newContext: C2) =
		copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Creates a new builder that builds both the stack and the content inside
	  * @param contentFactory A factory that produces content factories
	  * @tparam F Type of contextual content factories
	  * @return A new stack builder that uses the same context as in this factory
	  */
	def build[F](contentFactory: Ccff[N, F]) = new ContextualStackBuilder(this, contentFactory)
	
	/**
	  * Creates a new stack of items
	  * @param content Content to attach to this stack
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R],
										  direction: Axis2D = Y, layout: StackLayout = Fit,
										  cap: StackLength = StackLength.fixedZero,
										  customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false) =
		stackFactory(content, direction, layout,
			if (areRelated) context.smallStackMargin else context.stackMargin, cap, customDrawers)
	
	/**
	  * Creates a new stack of items
	  * @param content Content to attach to this stack
	  * @param margin Margin placed between stack elements
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def withMargin[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], margin: StackLength,
	                                           direction: Axis2D = Y, layout: StackLayout = Fit,
	                                           cap: StackLength = StackLength.fixedZero,
	                                           customDrawers: Vector[CustomDrawer] = Vector()) =
		stackFactory(content, direction, layout, margin, cap, customDrawers)
	
	/**
	  * Creates a new stack of items with no margin between them
	  * @param content Content to attach to this stack
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def withoutMargin[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], direction: Axis2D = Y,
												  layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
												  customDrawers: Vector[CustomDrawer] = Vector()) =
		stackFactory.withoutMargin(content, direction, layout, cap, customDrawers)
	
	/**
	  * Creates a new row of items
	  * @param content Content to attach to this stack
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def row[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], layout: StackLayout = Fit,
										cap: StackLength = StackLength.fixedZero,
										customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false) =
		apply(content, X, layout, cap, customDrawers, areRelated)
	
	/**
	  * Creates a new column of items
	  * @param content Content to attach to this stack
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def column[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], layout: StackLayout = Fit,
										   cap: StackLength = StackLength.fixedZero,
										   customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false) =
		apply(content, Y, layout, cap, customDrawers, areRelated)
	
	/**
	  * Creates a new stack where each item belongs to a shared segment (so that they are aligned with another
	  * stack's / container's items)
	  * @param group A segment group that determines content alignment
	  * @param content Components to place in this stack
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the stacked components should be considered closely related (affects margin)
	  *                   (default = false)
	  * @tparam R Type of an individual component creation result
	  * @return A new stack
	  */
	def segmented[R](group: SegmentGroup, content: Seq[OpenComponent[ReachComponentLike, R]], layout: StackLayout = Fit,
					 cap: StackLength = StackLength.fixedZero, customDrawers: Vector[CustomDrawer] = Vector(),
					 areRelated: Boolean = false) =
		stackFactory.segmented(group, content, layout,
			if (areRelated) context.stackMargin else context.smallStackMargin, cap, customDrawers)
	
	/**
	  * Creates a new stack that contains two items
	  * @param content Items to place in this stack
	  * @param alignment Alignment to use when placing the items. The direction of the alignment determines the
	  *                  position of the <b>first</b> item in the <i>content</i> parameter. Eg. Left alignment means
	  *                  that the first item will be placed at the left side and the second item on the right.
	  *                  Bottom alignment means that the first item will be placed at the bottom and the second at
	  *                  the top.
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @param forceFitLayout Whether layout should always be set to <i>Fit</i>, regardless of alignment
	  * @tparam C Type of the components
	  * @tparam R Type of additional creation result
	  * @return A new stack with the two items in it
	  */
	def forPair[C <: ReachComponentLike, R](content: OpenComponent[Pair[C], R], alignment: Alignment,
											cap: StackLength = StackLength.fixedZero,
											customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false,
											forceFitLayout: Boolean = false): ComponentWrapResult[Stack[C], Vector[C], R] =
		stackFactory.forPair(content, alignment,
			if (areRelated) context.smallStackMargin else context.stackMargin, cap, customDrawers,
			forceFitLayout)
}

class StackBuilder[+F](factory: StackFactory, contentFactory: ComponentFactoryFactory[F])
{
	// IMPLICIT	----------------------------
	
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	
	// OTHER	-----------------------------
	
	/**
	  * Builds a new stack of items
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param margin Margin placed between each component (default = any margin, preferring 0)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param fill A function for filling this stack
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def apply[C <: ReachComponentLike, R](direction: Axis2D = Y, layout: StackLayout = Fit,
										  margin: StackLength = StackLength.any,
										  cap: StackLength = StackLength.fixedZero,
										  customDrawers: Vector[CustomDrawer] = Vector())
										 (fill: F => ComponentCreationResult[Vector[C], R]) =
	{
		val content = Open.using(contentFactory)(fill)
		factory(content, direction, layout, margin, cap, customDrawers)
	}
	
	/**
	  * Builds a new stack that aligns its contents with another container's contents
	  * @param group A group that defines content alignment
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param margin Margin placed between each component (default = any margin, preferring 0)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param fill A function for filling this stack. Accepts an infinite iterator of component factories.
	  *             Each factory acquired with next() must only be used once (for one component).
	  * @tparam R Type of component creation result
	  * @return A new stack
	  */
	def segmented[R](group: SegmentGroup, layout: StackLayout = Fit, margin: StackLength = StackLength.any,
					 cap: StackLength = StackLength.fixedZero, customDrawers: Vector[CustomDrawer] = Vector())
					(fill: Iterator[F] => ComponentCreationResult[IterableOnce[ReachComponentLike], R]) =
	{
		val content = Open.manyUsing(contentFactory) { factories =>
			fill(factories).mapComponent { _.iterator.map { ComponentCreationResult(_) } }
		}
		factory.segmented(group, content.component, layout, margin, cap, customDrawers).withResult(content.result)
	}
	
	/**
	  * Builds a new stack that contains two items
	  * @param alignment Alignment to use when placing the items. The direction of the alignment determines the
	  *                  position of the <b>first</b> item in the <i>content</i> parameter. Eg. Left alignment means
	  *                  that the first item will be placed at the left side and the second item on the right.
	  *                  Bottom alignment means that the first item will be placed at the bottom and the second at
	  *                  the top.
	  * @param margin Margin placed between the items (default = any, preferring 0)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param forceFitLayout Whether layout should always be set to <i>Fit</i>, regardless of alignment
	  * @param fill A function for filling this stack
	  * @tparam C Type of the components
	  * @tparam R Type of additional creation result
	  * @return A new stack with the two items in it
	  */
	def forPair[C <: ReachComponentLike, R](alignment: Alignment, margin: StackLength = StackLength.any,
											cap: StackLength = StackLength.fixedZero,
											customDrawers: Vector[CustomDrawer] = Vector(),
											forceFitLayout: Boolean = false)
										   (fill: F => ComponentCreationResult[Pair[C], R]) =
	{
		val content = Open.using(contentFactory)(fill)
		factory.forPair(content, alignment, margin, cap, customDrawers, forceFitLayout)
	}
}

class ContextualStackBuilder[N <: BaseContext, +F](stackFactory: ContextualStackFactory[N], contentFactory: Ccff[N, F])
{
	private implicit def canvas: ReachCanvas = stackFactory.stackFactory.parentHierarchy.top
	
	private def context = stackFactory.context
	
	/**
	  * Creates a new stack of items
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @param fill A function for creating the components that will be placed in this stack
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def apply[C <: ReachComponentLike, R](direction: Axis2D = Y, layout: StackLayout = Fit,
										  cap: StackLength = StackLength.fixedZero,
										  customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
										 (fill: F => ComponentCreationResult[Vector[C], R]) =
	{
		val content = Open.withContext(stackFactory.context)(contentFactory)(fill)
		stackFactory(content, direction, layout, cap, customDrawers, areRelated)
	}
	
	/**
	  * Creates a new stack of items
	  * @param margin Margin placed between stack elements
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param fill A function for creating the components that will be placed in this stack
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def withMargin[C <: ReachComponentLike, R](margin: StackLength, direction: Axis2D = Y, layout: StackLayout = Fit,
	                                           cap: StackLength = StackLength.fixedZero,
	                                           customDrawers: Vector[CustomDrawer] = Vector())
	                                          (fill: F => ComponentCreationResult[Vector[C], R]) =
	{
		val content = Open.withContext(stackFactory.context)(contentFactory)(fill)
		stackFactory.withMargin(content, margin, direction, layout, cap, customDrawers)
	}
	
	/**
	  * Creates a new stack of items with no margin between them
	  * @param direction Axis along which the components are stacked / form a line (default = Y = column)
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param fill A function for creating the components that will be placed in this stack
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def withoutMargin[C <: ReachComponentLike, R](direction: Axis2D = Y, layout: StackLayout = Fit,
												  cap: StackLength = StackLength.fixedZero,
												  customDrawers: Vector[CustomDrawer] = Vector())
												 (fill: F => ComponentCreationResult[Vector[C], R]) =
	{
		val content = Open.withContext(stackFactory.context)(contentFactory)(fill)
		stackFactory.withoutMargin(content, direction, layout, cap, customDrawers)
	}
	
	/**
	  * Creates a new row of items
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @param fill A function for creating the components that will be placed in this stack
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def row[C <: ReachComponentLike, R](layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
										customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
									   (fill: F => ComponentCreationResult[Vector[C], R]) =
		apply(X, layout, cap, customDrawers, areRelated)(fill)
	
	/**
	  * Creates a new column of items
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @param fill A function for creating the components that will be placed in this stack
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def column[C <: ReachComponentLike, R](layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
										   customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
										  (fill: F => ComponentCreationResult[Vector[C], R]) =
		apply(Y, layout, cap, customDrawers, areRelated)(fill)
	
	/**
	  * Builds a new segmented stack of items. The items in this stack are aligned with some other container's items
	  * @param group A group that determines item alignment
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers  Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @param fill A function for creating the components that will be placed in this stack. Accepts an infinite
	  *             iterator of component factories. Each factory acquired with next() must only be used once
	  *             (for creation of a single component)
	  * @tparam R Type of component creation result
	  * @return A new stack
	  */
	def segmented[R](group: SegmentGroup, layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
					 customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
					(fill: Iterator[F] => ComponentCreationResult[IterableOnce[ReachComponentLike], R]) =
	{
		val content = Open.withContext(context).many(contentFactory) { factories =>
			fill(factories).mapComponent { _.iterator.map { ComponentCreationResult(_) } }
		}
		stackFactory.segmented(group, content.component, layout, cap, customDrawers, areRelated).withResult(content.result)
	}
	
	/**
	  * Creates a new stack that contains two items
	  * @param alignment Alignment to use when placing the items. The direction of the alignment determines the
	  *                  position of the <b>first</b> item in the <i>content</i> parameter. Eg. Left alignment means
	  *                  that the first item will be placed at the left side and the second item on the right.
	  *                  Bottom alignment means that the first item will be placed at the bottom and the second at
	  *                  the top.
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @param forceFitLayout Whether layout should always be set to <i>Fit</i>, regardless of alignment
	  * @param fill A function for creating the components that will be placed in this stack
	  * @tparam C Type of the components
	  * @tparam R Type of additional creation result
	  * @return A new stack with the two items in it
	  */
	def pair[C <: ReachComponentLike, R](alignment: Alignment, cap: StackLength = StackLength.fixedZero,
										 customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false,
										 forceFitLayout: Boolean = false)
										(fill: F => ComponentCreationResult[Pair[C], R]): ComponentWrapResult[Stack[C], Vector[C], R] =
	{
		val content = Open.withContext(stackFactory.context)(contentFactory)(fill)
		stackFactory.forPair(content, alignment, cap, customDrawers, areRelated, forceFitLayout)
	}
}
