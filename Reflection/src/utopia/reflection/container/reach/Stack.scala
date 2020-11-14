package utopia.reflection.container.reach

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.factory.{ComponentFactoryFactory, ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.{Center, Fit, Leading, Trailing}
import utopia.reflection.container.stack.template.layout.StackLike2
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackLength

object Stack extends ContextInsertableComponentFactoryFactory[BaseContextLike, StackFactory, ContextualStackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = StackFactory(hierarchy)
}

case class StackFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[BaseContextLike, ContextualStackFactory]
{
	// COMPUTED	--------------------------------
	
	/**
	  * Creates a new stack builder
	  * @param contentFactory Factory used for stack content factories
	  * @tparam F Type of content factory
	  * @return A stack builder
	  */
	def build[F](contentFactory: ComponentFactoryFactory[F]) = new StackBuilder[F](this, contentFactory)
	
	
	// IMPLEMENTED	----------------------------
	
	override def withContext[N <: BaseContextLike](context: N) =
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
		val stack = new Stack[C](parentHierarchy, content.component, direction, layout, margin, cap, customDrawers)
		content attachTo stack
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
	def forPair[C <: ReachComponentLike, R](content: OpenComponent[(_ <: C, _ <: C), R], alignment: Alignment,
											margin: StackLength = StackLength.any,
											cap: StackLength = StackLength.fixedZero,
											customDrawers: Vector[CustomDrawer] = Vector(),
											forceFitLayout: Boolean = false) =
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
							case Positive => Trailing
							case Negative => Leading
						}
					case None => Center
				}
				(X, horizontal, layout)
			case None => (Y, alignment.verticalDirection.getOrElse(Positive), Center)
		}
		val orderedContent = content.mapComponent { case (a, b) =>
			sign match
			{
				case Positive => Vector(b, a)
				case Negative => Vector(a, b)
			}
		}
		// Creates the stack
		apply(orderedContent, axis, if (forceFitLayout) Fit else layout, margin, cap, customDrawers)
	}
}

case class ContextualStackFactory[N <: BaseContextLike](stackFactory: StackFactory, context: N)
	extends ContextualComponentFactory[N, BaseContextLike, ContextualStackFactory]
{
	// IMPLEMENTED	--------------------------------
	
	override def withContext[C2 <: BaseContextLike](newContext: C2) = copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Creates a new builder that builds both the stack and the content inside
	  * @param contentFactory A factory that produces content factories
	  * @tparam F Type of contextual content factories
	  * @return A new stack builder that uses the same context as in this factory
	  */
	def build[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
	(contentFactory: ContextInsertableComponentFactoryFactory[_ >: N, _, F]) =
		new ContextualStackBuilder(this, contentFactory)
	
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
			if (areRelated) context.relatedItemsStackMargin else context.defaultStackMargin, cap, customDrawers)
	
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
		stackFactory(content, direction, layout, StackLength.fixedZero, cap, customDrawers)
	
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
	def forPair[C <: ReachComponentLike, R](content: OpenComponent[(_ <: C, _ <: C), R], alignment: Alignment,
											cap: StackLength = StackLength.fixedZero,
											customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false,
											forceFitLayout: Boolean = false) =
		stackFactory.forPair(content, alignment,
			if (areRelated) context.relatedItemsStackMargin else context.defaultStackMargin, cap, customDrawers,
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
										   (fill: F => ComponentCreationResult[(C, C), R]) =
	{
		val content = Open.using(contentFactory)(fill)
		factory.forPair(content, alignment, margin, cap, customDrawers, forceFitLayout)
	}
}

class ContextualStackBuilder[N <: BaseContextLike, +F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
(stackFactory: ContextualStackFactory[N], contentFactory: ContextInsertableComponentFactoryFactory[_ >: N, _, F])
{
	private implicit def canvas: ReachCanvas = stackFactory.stackFactory.parentHierarchy.top
	
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
										 (fill: F[N] => ComponentCreationResult[Vector[C], R]) =
	{
		val content = Open.withContext(contentFactory, stackFactory.context)(fill)
		stackFactory(content, direction, layout, cap, customDrawers, areRelated)
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
												 (fill: F[N] => ComponentCreationResult[Vector[C], R]) =
	{
		val content = Open.withContext(contentFactory, stackFactory.context)(fill)
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
									   (fill: F[N] => ComponentCreationResult[Vector[C], R]) =
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
										  (fill: F[N] => ComponentCreationResult[Vector[C], R]) =
		apply(Y, layout, cap, customDrawers, areRelated)(fill)
	
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
										(fill: F[N] => ComponentCreationResult[(_ <: C, _ <: C), R]) =
	{
		val content = Open.withContext(contentFactory, stackFactory.context)(fill)
		stackFactory.forPair(content, alignment, cap, customDrawers, areRelated, forceFitLayout)
	}
}

/**
  * A static Reach implementation of the stack concept
  * @author Mikko Hilpinen
  * @since 11.10.2020, v2
  */
class Stack[C <: ReachComponentLike](override val parentHierarchy: ComponentHierarchy,
									 override val components: Vector[C], override val direction: Axis2D,
									 override val layout: StackLayout, override val margin: StackLength,
									 override val cap: StackLength, override val customDrawers: Vector[CustomDrawer])
	extends CustomDrawReachComponent with StackLike2[C]
{
	override def children = components
}
