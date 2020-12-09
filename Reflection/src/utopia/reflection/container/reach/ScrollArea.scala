package utopia.reflection.container.reach

import utopia.flow.event.ChangeListener
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.LinearAcceleration
import utopia.genesis.shape.shape2D.{Bounds, Size}
import utopia.reflection.color.ComponentColor
import utopia.reflection.component.context.ScrollingContextLike
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.reflection.component.reach.factory.ContextInsertableComponentFactoryFactory.ContextualBuilderContentFactory
import utopia.reflection.component.reach.factory.{BuilderFactory, ComponentFactoryFactory, ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, SimpleFilledBuilderFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.container.stack.template.scrolling.ScrollAreaLike2
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.stack.modifier.MaxOptimalSizeModifier
import utopia.reflection.util.ComponentCreationDefaults

object ScrollArea extends ContextInsertableComponentFactoryFactory[Any, ScrollAreaFactory, ContextualScrollAreaFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ScrollAreaFactory(hierarchy)
}

class ScrollAreaFactory(val parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[Any, ContextualScrollAreaFactory] with BuilderFactory[ScrollAreaBuilder]
		with SimpleFilledBuilderFactory[ContextualFilledScrollAreaBuilder]
{
	// IMPLEMENTED	----------------------------------
	
	override def withContext[N <: Any](context: N) =
		ContextualScrollAreaFactory(this, context)
	
	override def build[F](contentFactory: ComponentFactoryFactory[F]) =
		new ScrollAreaBuilder[F](this, contentFactory)
	
	protected def makeBuilder[NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(background: ComponentColor, contentContext: NC, contentFactory: ContextualBuilderContentFactory[NC, F]) =
		new ContextualFilledScrollAreaBuilder[NC, F](this, background, contentContext, contentFactory)
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Creates a new scroll area
	  * @param content Content that will be placed inside this area
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalSize Limits placed on the optimal size of this scroll area (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param context Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll area
	  */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R], scrollBarMargin: Size = Size.zero,
										  maxOptimalSize: Option[Size] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)(implicit context: ScrollingContextLike) =
	{
		val area = new ScrollArea(parentHierarchy, content.component, context.actorHandler, context.scrollBarDrawer,
			context.scrollBarWidth, scrollBarMargin, context.scrollPerWheelClick, context.scrollFriction,
			customDrawers, limitsToContentSize, context.scrollBarIsInsideContent)
		maxOptimalSize.foreach { maxOptimal => area.addConstraint(MaxOptimalSizeModifier(maxOptimal)) }
		content.attachTo(area)
	}
}

case class ContextualScrollAreaFactory[N](factory: ScrollAreaFactory, context: N)
	extends ContextualComponentFactory[N, Any, ContextualScrollAreaFactory]
{
	// IMPLEMENTED	--------------------------
	
	override def withContext[N2 <: Any](newContext: N2) = copy(context = newContext)
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return A version of this factory which doesn't utilize component creation context
	  */
	def withoutContext = factory
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param contentFactory Scroll area content factory factory
	  * @tparam F Type of content component factory
	  * @return A new scroll area builder
	  */
	def build[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]](contentFactory: ContextualBuilderContentFactory[N, F]) =
		new ContextualScrollAreaBuilder[N, F](factory, context, contentFactory)
}

class ScrollAreaBuilder[+F](factory: ScrollAreaFactory, contentFactory: ComponentFactoryFactory[F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	  * Creates a new scroll area
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalSize Limits placed on the optimal size of this scroll area (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param fill A function for producing scroll area content. Accepts a component factory.
	  * @param context Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll area
	  */
	def apply[C <: ReachComponentLike, R](scrollBarMargin: Size = Size.zero, maxOptimalSize: Option[Size] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)
										 (fill: F => ComponentCreationResult[C, R])
										 (implicit context: ScrollingContextLike) =
	{
		val content = Open.using(contentFactory)(fill)
		factory(content, scrollBarMargin, maxOptimalSize, customDrawers, limitsToContentSize)
	}
}

class ContextualScrollAreaBuilder[N, +F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
(factory: ScrollAreaFactory, context: N, contentFactory: ContextualBuilderContentFactory[N, F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	  * Creates a new scroll area
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalSize Limits placed on the optimal size of this scroll area (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param fill A function for producing scroll area content. Accepts a component factory.
	  * @param scrollingContext Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll area
	  */
	def apply[C <: ReachComponentLike, R](scrollBarMargin: Size = Size.zero, maxOptimalSize: Option[Size] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)
										 (fill: F[N] => ComponentCreationResult[C, R])
										 (implicit scrollingContext: ScrollingContextLike) =
	{
		val content = Open.withContext(contentFactory, context)(fill)
		factory(content, scrollBarMargin, maxOptimalSize, customDrawers, limitsToContentSize)
	}
}

class ContextualFilledScrollAreaBuilder[NC, +F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
(factory: ScrollAreaFactory, background: ComponentColor, contentContext: NC,
 contentFactory: ContextualBuilderContentFactory[NC, F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	  * Creates a new scroll area
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalSize Limits placed on the optimal size of this scroll area (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param fill A function for producing scroll area content. Accepts a component factory.
	  * @param scrollContext Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll area
	  */
	def apply[C <: ReachComponentLike, R](scrollBarMargin: Size = Size.zero, maxOptimalSize: Option[Size] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)
										 (fill: F[NC] => ComponentCreationResult[C, R])
										 (implicit scrollContext: ScrollingContextLike) =
	{
		val content = Open.withContext(contentFactory, contentContext)(fill)
		factory(content, scrollBarMargin, maxOptimalSize, customDrawers :+ BackgroundDrawer(background),
			limitsToContentSize)
	}
}

/**
  * A component wrapper which allows content scrolling
  * @author Mikko Hilpinen
  * @since 7.12.2020, v2
  */
class ScrollArea(override val parentHierarchy: ComponentHierarchy, override val content: ReachComponentLike,
				 actorHandler: ActorHandler, barDrawer: ScrollBarDrawerLike,
				 override val scrollBarWidth: Double = ComponentCreationDefaults.scrollBarWidth,
				 override val scrollBarMargin: Size = Size.zero,
				 scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
				 override val friction: LinearAcceleration = ComponentCreationDefaults.scrollFriction,
				 additionalDrawers: Vector[CustomDrawer] = Vector(),
				 override val limitsToContentSize: Boolean = false,
				 override val scrollBarIsInsideContent: Boolean = true)
	extends CustomDrawReachComponent with ScrollAreaLike2[ReachComponentLike]
{
	// ATTRIBUTES	----------------------------
	
	override val customDrawers = scrollBarDrawerToCustomDrawer(barDrawer) +: additionalDrawers
	
	
	// INITIAL CODE	----------------------------
	
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	sizePointer.addListener(ChangeListener.onAnyChange { updateScrollBarBounds() })
	
	
	// IMPLEMENTED	----------------------------
	
	override def axes = Axis2D.values
	
	// override def repaint(bounds: Bounds) = parentHierarchy.repaint(bounds + position)
}
