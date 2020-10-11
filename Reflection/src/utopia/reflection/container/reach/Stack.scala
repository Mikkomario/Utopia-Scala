package utopia.reflection.container.reach

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{BuilderFactory, ComponentFactoryFactory, CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.stack.template.layout.StackLike2
import utopia.reflection.shape.stack.StackLength

object Stack extends ComponentFactoryFactory[StackFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = StackFactory(hierarchy)
}

case class StackFactory(parentHierarchy: ComponentHierarchy) extends BuilderFactory[StackBuilder]
{
	// IMPLEMENTED	----------------------------
	
	override def builder[CF](contentFactory: ComponentFactoryFactory[CF]) =
		StackBuilder(this, contentFactory)
	
	
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

case class StackBuilder[+CF](stackFactory: StackFactory, contentFactory: ComponentFactoryFactory[CF])
{
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
	  * @param context Implicit component creation context
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def apply[C <: ReachComponentLike, R](direction: Axis2D = Y, layout: StackLayout = Fit,
										  cap: StackLength = StackLength.fixedZero,
										  customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
										 (fill: CF => ComponentCreationResult[Vector[C], R])
										 (implicit context: BaseContextLike) =
	{
		val content = Open.using(contentFactory)(fill)(stackFactory.parentHierarchy.top)
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
	  * @param context Implicit component creation context
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def row[C <: ReachComponentLike, R](layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
										customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
									   (fill: CF => ComponentCreationResult[Vector[C], R])
									   (implicit context: BaseContextLike) =
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
	  * @param context Implicit component creation context
	  * @tparam C Type of wrapped component
	  * @tparam R Type of component creation result
	  * @return This stack, along with the created components and possible additional result value
	  */
	def column[C <: ReachComponentLike, R](layout: StackLayout = Fit, cap: StackLength = StackLength.fixedZero,
										   customDrawers: Vector[CustomDrawer] = Vector(), areRelated: Boolean = false)
										  (fill: CF => ComponentCreationResult[Vector[C], R])
										  (implicit context: BaseContextLike) =
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
