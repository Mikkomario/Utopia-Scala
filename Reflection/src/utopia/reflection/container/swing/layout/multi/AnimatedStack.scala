package utopia.reflection.container.swing.layout.multi

import utopia.firmament.component.AreaOfItems
import utopia.firmament.context.base.StaticBaseContext
import utopia.firmament.context.{AnimationContext, ComponentCreationDefaults}
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.StackLength
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.util.Fps
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reflection.component.swing.animation.AnimatedVisibility
import utopia.reflection.component.swing.template.SwingComponentRelated
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.AnimatedChangesContainer
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object AnimatedStack
{
	/**
	  * Creates an animated stack using a component creation context
	  * @param direction Direction of this stack
	  * @param items Items placed inside this stack initially (default = empty)
	  * @param layout Layout used for items breadth-wise (default = Fit)
	  * @param cap Cap placed at each end of stack (default = no cap)
	  * @param itemsAreRelated Whether items should be considered related (affects margin) (default = false)
	  * @param ac Component animation context (implicit)
	  * @param bc Base component creation context (implicit)
	  * @param exc Execution context
	  * @tparam C Type of stacked component
	  * @return A new stack that uses animations
	  */
	def contextual[C <: AwtStackable](direction: Axis2D, items: Vector[C] = Vector(), layout: StackLayout = Fit,
									  cap: StackLength = StackLength.fixedZero,
									  itemsAreRelated: Boolean = false)
	                                 (implicit ac: AnimationContext, bc: StaticBaseContext, exc: ExecutionContext) =
	{
		val stack = new AnimatedStack[C](ac.actorHandler, direction,
			if (itemsAreRelated) bc.smallStackMargin else bc.stackMargin, cap, layout,
			ac.animationDuration, ac.maxAnimationRefreshRate, ac.useFadingInAnimations)
		stack ++= items
		stack
	}
	
	/**
	  * Creates an animated stack column using a component creation context
	  * @param items Items placed inside this stack initially (default = empty)
	  * @param layout Layout used for items breadth-wise (default = Fit)
	  * @param cap Cap placed at each end of stack (default = no cap)
	  * @param itemsAreRelated Whether items should be considered related (affects margin) (default = false)
	  * @param ac Component animation context (implicit)
	  * @param bc Base component creation context (implicit)
	  * @param exc Execution context
	  * @tparam C Type of stacked component
	  * @return A new stack that uses animations
	  */
	def contextualColumn[C <: AwtStackable](items: Vector[C] = Vector(), layout: StackLayout = Fit,
											cap: StackLength = StackLength.fixedZero, itemsAreRelated: Boolean = false)
										   (implicit ac: AnimationContext, bc: StaticBaseContext, exc: ExecutionContext) =
		contextual(Y, items, layout, cap, itemsAreRelated)
	
	/**
	  * Creates an animated stack row using a component creation context
	  * @param items Items placed inside this stack initially (default = empty)
	  * @param layout Layout used for items breadth-wise (default = Fit)
	  * @param cap Cap placed at each end of stack (default = no cap)
	  * @param itemsAreRelated Whether items should be considered related (affects margin) (default = false)
	  * @param ac Component animation context (implicit)
	  * @param bc Base component creation context (implicit)
	  * @param exc Execution context
	  * @tparam C Type of stacked component
	  * @return A new stack that uses animations
	  */
	def contextualRow[C <: AwtStackable](items: Vector[C] = Vector(), layout: StackLayout = Fit,
	                                     cap: StackLength = StackLength.fixedZero, itemsAreRelated: Boolean = false)
	                                    (implicit ac: AnimationContext, bc: StaticBaseContext, exc: ExecutionContext) =
		contextual(X, items, layout, cap, itemsAreRelated)
}

/**
  * This version of stack uses animations when adding / removing components
  * @author Mikko Hilpinen
  * @since 18.4.2020, v1.2
  */
class AnimatedStack[C <: AwtStackable](actorHandler: ActorHandler, direction: Axis2D,
                                       margin: StackLength = StackLength.any,
                                       cap: StackLength = StackLength.fixedZero, layout: StackLayout = Fit,
                                       animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
                                       maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate,
                                       fadingIsEnabled: Boolean = true)(implicit val executionContext: ExecutionContext)
	extends AnimatedChangesContainer[C, Stack[AnimatedVisibility[C]]](
		new Stack[AnimatedVisibility[C]](direction, margin, cap, layout), actorHandler, Some(direction), animationDuration,
		maxRefreshRate, fadingIsEnabled)
		with SwingComponentRelated with MutableCustomDrawableWrapper with AreaOfItems[C] with AwtContainerRelated
{
	// IMPLEMENTED	-------------------------
	
	override def children = components
	
	override def component = container.component
	
	override def drawable = container
	
	override def areaOf(item: C) = wrappers.find { _.display == item }.flatMap { container.areaOf }
	
	override def itemNearestTo(relativePoint: Point) = container.itemNearestTo(relativePoint).map { _.display }
}
