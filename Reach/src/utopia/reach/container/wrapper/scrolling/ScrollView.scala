package utopia.reach.container.wrapper.scrolling

import utopia.firmament.component.container.single.ScrollViewLike
import utopia.firmament.context.{ComponentCreationDefaults, ScrollingContext}
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.firmament.model.stack.modifier.MaxOptimalLengthModifier
import utopia.flow.event.listener.ChangeListener
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.{Bounds, Size}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.{FromGenericContextFactory, GenericContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}
import utopia.reach.container.wrapper.{ContextualWrapperContainerFactory, NonContextualWrapperContainerFactory, WrapperContainerFactory}

import scala.language.implicitConversions

object ScrollView extends Cff[ScrollViewFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ScrollViewFactory(hierarchy)
}

trait ScrollViewFactoryLike[+Repr]
	extends WrapperContainerFactory[ScrollView, ReachComponentLike] with CustomDrawableFactory[Repr]
{
	// ABSTRACT ---------------------------
	
	def scrollContext: ScrollingContext
	
	def axis: Axis2D
	def scrollBarMargin: Size
	def maxOptimalLength: Option[Double]
	def limitsToContentSize: Boolean
	
	/**
	  * @param axis The axis along which the content is scrolled
	  * @return Copy of this factory with the specified axis
	  */
	def withAxis(axis: Axis2D): Repr
	/**
	  * @param margin Margins placed around the scroll bars (wider edge margin as X and thinner edge margins as Y)
	  *               (default = 0x0)
	  * @return Copy of this factory with the specified margins
	  */
	def withScrollBarMargin(margin: Size): Repr
	/**
	  * @param max Largest allowed optimal length for this view
	  * @return Copy of this factory with the specified maximum applied
	  */
	def withMaxOptimalLength(max: Double): Repr
	/**
	  * @param limits Whether the size of this view should be limited to the length of the content
	  * @return Copy of this factory with the specified setting in place
	  */
	def withLimitsToContentSize(limits: Boolean): Repr
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Copy of this factory that builds horizontal scroll views
	  */
	def horizontal = withAxis(X)
	
	/**
	  * @return Copy of this factory that limits the scroll view length to content length
	  */
	def limitedToContentSize = withLimitsToContentSize(limits = true)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R]): ComponentWrapResult[ScrollView, C, R] = {
		val view = new ScrollView(parentHierarchy, content.component, scrollContext.actorHandler,
			scrollContext.scrollBarDrawer, axis, scrollContext.scrollBarWidth, scrollBarMargin,
			scrollContext.scrollPerWheelClick, scrollContext.scrollFriction,
			customDrawers, limitsToContentSize, scrollContext.scrollBarIsInsideContent)
		maxOptimalLength.foreach { maxOptimal => view.addConstraintOver(axis)(MaxOptimalLengthModifier(maxOptimal)) }
		content.attachTo(view)
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param long Margin to place along the long edge of this scroll view
	  * @param ends Margin to place at each end of the scroll bar
	  * @return Copy of this factory with the specified margins
	  */
	def withScrollBarMargin(long: Double, ends: Double): Repr = withScrollBarMargin(Size(long, ends))
	def mapScrollBarMargin(f: Size => Size) = withScrollBarMargin(f(scrollBarMargin))
	
	/**
	  * @param margin Margin to place between the long (scrolling) edge of this view and the painted scroll bar
	  * @return Copy of this factory with the specified margin in place
	  */
	def withLongEdgeScrollBarMargin(margin: Double) = mapScrollBarMargin { _.withX(margin) }
	/**
	  * @param margin Margin to place at each scroll bar end
	  * @return Copy of this factory with the specified margins
	  */
	def withEndScrollBarMargins(margin: Double) = mapScrollBarMargin { _.withY(margin) }
}

object ScrollViewFactory
{
	implicit def autoInitialize[F](f: UninitializedScrollViewFactory[F])(implicit c: ScrollingContext): F =
		f.initialized
}

trait UninitializedScrollViewFactory[+Initialized]
{
	// COMPUTED ----------------------------
	
	/**
	  * @param context Implicit scrolling context
	  * @return An initialized version of this factory
	  */
	def initialized(implicit context: ScrollingContext) = withScrollContext(context)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param scrollContext Scrolling context to use
	  * @return An initialized version of this factory
	  */
	def withScrollContext(scrollContext: ScrollingContext): Initialized
}

case class ScrollViewFactory(parentHierarchy: ComponentHierarchy)
	extends UninitializedScrollViewFactory[InitializedScrollViewFactory]
		with FromGenericContextFactory[Any, ContextualScrollViewFactory]
{
	// IMPLEMENTED  ------------------------
	
	override def withContext[N <: Any](context: N): ContextualScrollViewFactory[N] =
		ContextualScrollViewFactory(parentHierarchy, context)
	
	/**
	  * @param scrollContext Scrolling context to use
	  * @return An initialized version of this factory
	  */
	override def withScrollContext(scrollContext: ScrollingContext) =
		InitializedScrollViewFactory(parentHierarchy)(scrollContext)
}

case class InitializedScrollViewFactory(parentHierarchy: ComponentHierarchy, axis: Axis2D = Y, scrollBarMargin: Size = Size.zero,
                             maxOptimalLength: Option[Double] = None, customDrawers: Vector[CustomDrawer] = Vector(),
                             limitsToContentSize: Boolean = false)
                            (implicit override val scrollContext: ScrollingContext)
	extends ScrollViewFactoryLike[InitializedScrollViewFactory]
		with NonContextualWrapperContainerFactory[ScrollView, ReachComponentLike]
		with FromGenericContextFactory[Any, InitializedContextualScrollViewFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: Any](context: N) =
		InitializedContextualScrollViewFactory(parentHierarchy, context, axis, scrollBarMargin, maxOptimalLength,
			customDrawers, limitsToContentSize)
	
	
	// OTHER	-----------------------------------
	
	override def withAxis(axis: Axis2D) = copy(axis = axis)
	override def withScrollBarMargin(margin: Size) = copy(scrollBarMargin = margin)
	override def withMaxOptimalLength(max: Double) = copy(maxOptimalLength = Some(max))
	override def withLimitsToContentSize(limits: Boolean) = copy(limitsToContentSize = limits)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) = copy(customDrawers = drawers)
}

case class ContextualScrollViewFactory[N](parentHierarchy: ComponentHierarchy, context: N)
	extends GenericContextualFactory[N, Any, ContextualScrollViewFactory]
		with UninitializedScrollViewFactory[InitializedContextualScrollViewFactory[N]]
{
	// IMPLEMENTED  -----------------------
	
	override def withContext[N2 <: Any](newContext: N2): ContextualScrollViewFactory[N2] = copy(context = newContext)
	
	/**
	  * @param scrollContext Scrolling context to use
	  * @return An initialized version of this factory
	  */
	override def withScrollContext(scrollContext: ScrollingContext) =
		InitializedContextualScrollViewFactory(parentHierarchy, context)(scrollContext)
}

case class InitializedContextualScrollViewFactory[N](parentHierarchy: ComponentHierarchy, context: N, axis: Axis2D = Y,
                                                     scrollBarMargin: Size = Size.zero,
                                                     maxOptimalLength: Option[Double] = None,
                                                     customDrawers: Vector[CustomDrawer] = Vector(),
                                                     limitsToContentSize: Boolean = false)
                                                    (implicit override val scrollContext: ScrollingContext)
	extends ScrollViewFactoryLike[InitializedContextualScrollViewFactory[N]]
		with ContextualWrapperContainerFactory[N, Any, ScrollView, ReachComponentLike, InitializedContextualScrollViewFactory]
{
	// IMPLEMENTED  --------------------------
	
	override def withContext[N2 <: Any](newContext: N2) =
		copy(context = newContext)
	
	override def withAxis(axis: Axis2D) = copy(axis = axis)
	override def withScrollBarMargin(margin: Size) =
		copy(scrollBarMargin = margin)
	override def withMaxOptimalLength(max: Double) =
		copy(maxOptimalLength = Some(max))
	override def withLimitsToContentSize(limits: Boolean) =
		copy(limitsToContentSize = limits)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		copy(customDrawers = drawers)
}

/**
  * A component wrapper which allows scrolling along one axis
  * @author Mikko Hilpinen
  * @since 9.12.2020, v0.1
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
	extends CustomDrawReachComponent with ScrollViewLike[ReachComponentLike]
{
	// ATTRIBUTES	----------------------------
	
	override val customDrawers = scrollBarDrawerToCustomDrawer(barDrawer) +: additionalDrawers
	
	
	// INITIAL CODE	----------------------------
	
	// WET WET (see ScrollArea)
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	sizePointer.addListener(ChangeListener.onAnyChange { updateScrollBarBounds() })
	content.sizePointer.addContinuousAnyChangeListener { updateScrollBarBounds(repaintAfter = true) }
	
	
	// IMPLEMENTED	----------------------------
	
	// WET WET (from ScrollArea)
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = clipZone match {
		case Some(clip) =>
			clip.overlapWith(bounds).foreach { c => super.paintWith(drawer.clippedToBounds(c), Some(c)) }
		case None => super.paintWith(drawer.clippedToBounds(bounds), Some(bounds))
	}
}