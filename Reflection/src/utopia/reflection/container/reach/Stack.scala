package utopia.reflection.container.reach

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.stack.template.layout.StackLike2
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.stack.StackLength

object Stack extends ContextInsertableComponentFactoryFactory[BaseContextLike, StackFactory, ContextualStackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = StackFactory(hierarchy)
}

case class StackFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[BaseContextLike, ContextualStackFactory]
{
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
	def custom[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], direction: Axis2D = Y,
										   layout: StackLayout = Fit, margin: StackLength = StackLength.any,
										   cap: StackLength = StackLength.fixedZero,
										   customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val stack = new Stack[C](parentHierarchy, content.component, direction, layout, margin, cap, customDrawers)
		content attachTo stack
	}
	
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
	  * @param context Implicit component creation context
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R],
										  direction: Axis2D = Y, layout: StackLayout = Fit,
										  cap: StackLength = StackLength.fixedZero,
										  customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
										 (implicit context: BaseContextLike) =
		custom(content, direction, layout,
			if (areRelated) context.relatedItemsStackMargin else context.defaultStackMargin, cap, customDrawers)
	
	/**
	  * Creates a new row of items
	  * @param content Content to attach to this stack
	  * @param layout Layout used for handling lengths perpendicular to stack direction (breadth)
	  *               (default = Fit = All components have same breadth as this stack)
	  * @param cap Cap placed at each end of this stack (default = always 0)
	  * @param customDrawers Custom drawers attached to this stack (default = empty)
	  * @param areRelated Whether the components should be considered closely related (uses smaller margin)
	  *                   (default = false)
	  * @param context Implicit component creation context
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def row[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], layout: StackLayout = Fit,
										cap: StackLength = StackLength.fixedZero,
										customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
									   (implicit context: BaseContextLike) =
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
	  * @param context Implicit component creation context
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with contextual information
	  */
	def column[C <: ReachComponentLike, R](content: OpenComponent[Vector[C], R], layout: StackLayout = Fit,
										   cap: StackLength = StackLength.fixedZero,
										   customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
										  (implicit context: BaseContextLike) =
		apply(content, Y, layout, cap, customDrawers, areRelated)
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
	def builder[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
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
		stackFactory.custom(content, direction, layout,
			if (areRelated) context.relatedItemsStackMargin else context.defaultStackMargin, cap, customDrawers)
	
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
	override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
	
	override def children = components
}
