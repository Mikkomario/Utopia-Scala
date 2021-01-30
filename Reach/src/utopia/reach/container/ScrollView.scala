package utopia.reach.container

import utopia.flow.event.ChangeListener
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.LinearAcceleration
import utopia.genesis.shape.shape2D.{Bounds, Size}
import utopia.genesis.util.Drawer
import utopia.reach.component.factory.ContextInsertableComponentFactoryFactory.ContextualBuilderContentFactory
import utopia.reach.component.factory.{BuilderFactory, ComponentFactoryFactory, ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, SimpleFilledBuilderFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.color.ComponentColor
import utopia.reflection.component.context.ScrollingContextLike
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.reflection.container.stack.template.scrolling.ScrollViewLike2
import utopia.reflection.shape.stack.modifier.MaxOptimalLengthModifier
import utopia.reflection.util.ComponentCreationDefaults

object ScrollView extends ContextInsertableComponentFactoryFactory[Any, ScrollViewFactory, ContextualScrollViewFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ScrollViewFactory(hierarchy)
}

class ScrollViewFactory(val parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[Any, ContextualScrollViewFactory] with BuilderFactory[ScrollViewBuilder]
		with SimpleFilledBuilderFactory[ContextualFilledScrollViewBuilder]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: Any](context: N) =
		ContextualScrollViewFactory(this, context)
	
	override def build[FF](contentFactory: ComponentFactoryFactory[FF]) =
		new ScrollViewBuilder(this, contentFactory)
	
	protected def makeBuilder[NC, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(background: ComponentColor, contentContext: NC, contentFactory: ContextualBuilderContentFactory[NC, F]) =
		new ContextualFilledScrollViewBuilder[NC, F](this, background, contentContext, contentFactory)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Creates a new scroll view
	  * @param content Content that will be placed inside this area
	  * @param axis Axis along which the scrolling occurs (default = Y = Vertical scrolling)
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalLength Limits placed on the optimal length of this scroll view (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param context Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll view
	  */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R], axis: Axis2D = Y,
										  scrollBarMargin: Size = Size.zero, maxOptimalLength: Option[Double] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)(implicit context: ScrollingContextLike) =
	{
		val view = new ScrollView(parentHierarchy, content.component, context.actorHandler, context.scrollBarDrawer,
			axis, context.scrollBarWidth, scrollBarMargin, context.scrollPerWheelClick, context.scrollFriction,
			customDrawers, limitsToContentSize, context.scrollBarIsInsideContent)
		maxOptimalLength.foreach { maxOptimal => view.addConstraintOver(axis)(MaxOptimalLengthModifier(maxOptimal)) }
		content.attachTo(view)
	}
}

case class ContextualScrollViewFactory[N](factory: ScrollViewFactory, context: N)
	extends ContextualComponentFactory[N, Any, ContextualScrollViewFactory]
{
	// COMPUTED ------------------------------
	
	/**
	 * @return A copy of this factory that doesn't use component creation context
	 */
	def withoutContext = factory
	
	
	// IMPLEMENTED  --------------------------
	
	override def withContext[N2 <: Any](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param contentFactory A factory for scroll view contents
	 * @tparam F Type of component creation factory
	 * @return A new scroll view builder
	 */
	def build[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
		(contentFactory: ContextualBuilderContentFactory[N, F]) =
		new ContextualScrollViewBuilder(factory, context, contentFactory)
}

class ScrollViewBuilder[+F](factory: ScrollViewFactory, contentFactory: ComponentFactoryFactory[F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	  * Creates a new scroll view
	  * @param axis Axis along which the scrolling occurs (default = Y = Vertical scrolling)
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalLength Limits placed on the optimal length of this scroll view (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param fill A function for creating the contents of this scroll view. Accepts component creation factory.
	  * @param context Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll view
	  */
	def apply[C <: ReachComponentLike, R](axis: Axis2D = Y, scrollBarMargin: Size = Size.zero,
										  maxOptimalLength: Option[Double] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)
										 (fill: F => ComponentCreationResult[C, R])
										 (implicit context: ScrollingContextLike) =
	{
		val content = Open.using(contentFactory)(fill)
		factory(content, axis, scrollBarMargin, maxOptimalLength, customDrawers, limitsToContentSize)
	}
}

class ContextualScrollViewBuilder[N, +F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
(factory: ScrollViewFactory, context: N, contentFactory: ContextualBuilderContentFactory[N, F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	  * Creates a new scroll view
	  * @param axis Axis along which the scrolling occurs (default = Y = Vertical scrolling)
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalLength Limits placed on the optimal length of this scroll view (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param fill A function for creating the contents of this scroll view. Accepts component creation factory.
	  * @param scrollingContext Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll view
	  */
	def apply[C <: ReachComponentLike, R](axis: Axis2D = Y, scrollBarMargin: Size = Size.zero,
										  maxOptimalLength: Option[Double] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)
										 (fill: F[N] => ComponentCreationResult[C, R])
										 (implicit scrollingContext: ScrollingContextLike) =
	{
		val content = Open.withContext(contentFactory, context)(fill)
		factory(content, axis, scrollBarMargin, maxOptimalLength, customDrawers, limitsToContentSize)
	}
}

class ContextualFilledScrollViewBuilder[NC, +F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
(factory: ScrollViewFactory, background: ComponentColor, contentContext: NC,
 contentFactory: ContextualBuilderContentFactory[NC, F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	/**
	  * Creates a new scroll view
	  * @param axis Axis along which the scrolling occurs (default = Y = Vertical scrolling)
	  * @param scrollBarMargin Margins placed around the scroll bars (wider margin as X and thinner edge margins as Y)
	  *                        (default = 0x0)
	  * @param maxOptimalLength Limits placed on the optimal length of this scroll view (optional)
	  * @param customDrawers Custom drawers assigned to this scroll area (default = empty)
	  * @param limitsToContentSize Whether scroll area size should be limited to content size (default = false)
	  * @param fill A function for creating the contents of this scroll view. Accepts component creation factory.
	  * @param scrollingContext Contextual scrolling information
	  * @tparam C Type of component inside this scroll area
	  * @tparam R Type of additional creation result
	  * @return A new scroll view
	  */
	def apply[C <: ReachComponentLike, R](axis: Axis2D = Y, scrollBarMargin: Size = Size.zero,
										  maxOptimalLength: Option[Double] = None,
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)
										 (fill: F[NC] => ComponentCreationResult[C, R])
										 (implicit scrollingContext: ScrollingContextLike) =
	{
		val content = Open.withContext(contentFactory, contentContext)(fill)
		factory(content, axis, scrollBarMargin, maxOptimalLength, BackgroundDrawer(background) +: customDrawers,
			limitsToContentSize)
	}
}

/**
  * A component wrapper which allows scrolling along one axis
  * @author Mikko Hilpinen
  * @since 9.12.2020, v2
  */
class ScrollView(override val parentHierarchy: ComponentHierarchy, override val content: ReachComponentLike,
				 actorHandler: ActorHandler, barDrawer: ScrollBarDrawerLike, override val axis: Axis2D = Y,
				 override val scrollBarWidth: Double = ComponentCreationDefaults.scrollBarWidth,
				 override val scrollBarMargin: Size = Size.zero,
				 scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
				 override val friction: LinearAcceleration = ComponentCreationDefaults.scrollFriction,
				 additionalDrawers: Vector[CustomDrawer] = Vector(),
				 override val limitsToContentSize: Boolean = false,
				 override val scrollBarIsInsideContent: Boolean = true)
	extends CustomDrawReachComponent with ScrollViewLike2[ReachComponentLike]
{
	// ATTRIBUTES	----------------------------
	
	override val customDrawers = scrollBarDrawerToCustomDrawer(barDrawer) +: additionalDrawers
	
	
	// INITIAL CODE	----------------------------
	
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	sizePointer.addListener(ChangeListener.onAnyChange { updateScrollBarBounds() })
	
	
	// IMPLEMENTED	----------------------------
	
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = clipZone match
	{
		case Some(clip) => clip.within(bounds).foreach { c => super.paintWith(drawer, Some(c)) }
		case None => super.paintWith(drawer, Some(bounds))
	}
}