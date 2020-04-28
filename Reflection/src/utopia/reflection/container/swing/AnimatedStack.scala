package utopia.reflection.container.swing

import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.AreaOfItems
import utopia.reflection.component.context.{AnimationContextLike, BaseContextLike}
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.swing.{AnimatedVisibility, SwingComponentRelated}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.StackLength

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
									  itemsAreRelated: Boolean = false)(implicit ac: AnimationContextLike,
																		bc: BaseContextLike, exc: ExecutionContext) =
	{
		val stack = new AnimatedStack[C](ac.actorHandler, direction,
			if (itemsAreRelated) bc.relatedItemsStackMargin else bc.defaultStackMargin, cap, layout,
			ac.animationDuration, ac.useFadingInAnimations)
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
										   (implicit ac: AnimationContextLike, bc: BaseContextLike, exc: ExecutionContext) =
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
										(implicit ac: AnimationContextLike, bc: BaseContextLike, exc: ExecutionContext) =
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
									   animationDuration: FiniteDuration = 0.25.seconds,
									   fadingIsEnabled: Boolean = true)(implicit val executionContext: ExecutionContext)
	extends AnimatedChangesContainer[C, Stack[AnimatedVisibility[C]]](
		new Stack[AnimatedVisibility[C]](direction, margin, cap, layout), actorHandler, direction, animationDuration,
		fadingIsEnabled) with SwingComponentRelated with CustomDrawableWrapper with AreaOfItems[C]
{
	// IMPLEMENTED	-------------------------
	
	override def component = container.component
	
	override def drawable = container
	
	override def areaOf(item: C) = wrappers.find { _._2 == item }.flatMap {
		case (wrap, _) => container.areaOf(wrap) }
	
	override def itemNearestTo(relativePoint: Point) = container.itemNearestTo(relativePoint).map { _.display }
}
