package utopia.reach.container.wrapper.scrolling

import utopia.firmament.component.container.single.ScrollViewLike
import utopia.firmament.context.{ComponentCreationDefaults, ScrollingContext}
import utopia.firmament.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.flow.event.listener.ChangeListener
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.action.ActorHandler
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.Hsl
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.Dimensions
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}
import utopia.reach.container.wrapper.{ContextualWrapperContainerFactory, NonContextualWrapperContainerFactory}

import scala.language.implicitConversions

object ScrollView extends Cff[ScrollViewFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ScrollViewFactory(hierarchy)
}

trait ScrollViewFactoryLike[+Repr] extends ScrollWrapperFactoryLike[ScrollView, Repr]
{
	// ABSTRACT ---------------------------
	
	def axis: Axis2D
	
	/**
	  * @param axis The axis along which the content is scrolled
	  * @return Copy of this factory with the specified axis
	  */
	def withAxis(axis: Axis2D): Repr
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Copy of this factory that builds horizontal scroll views
	  */
	def horizontal = withAxis(X)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R]): ComponentWrapResult[ScrollView, C, R] = {
		val view = new ScrollView(parentHierarchy, content.component, scrollContext.actorHandler,
			scrollContext.scrollBarDrawer, axis, scrollContext.scrollBarWidth, scrollBarMargin,
			scrollContext.scrollPerWheelClick, scrollContext.scrollFriction,
			customDrawers, limitsToContentSize, scrollContext.scrollBarIsInsideContent)
		applyLengthConstraintsTo(view)
		content.attachTo(view)
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param max Largest allowed optimal length for this view
	  * @return Copy of this factory with the specified maximum applied
	  */
	def withMaxOptimalLength(max: Double) = mapMaxOptimalLengths { _.withDimension(axis, Some(max)) }
}

case class ScrollViewFactory(parentHierarchy: ComponentHierarchy)
	extends UninitializedScrollAreaFactory[InitializedScrollViewFactory]
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
                                        maxOptimalLengths: Dimensions[Option[Double]] = Dimensions(None).empty,
                                        customDrawers: Vector[CustomDrawer] = Vector(),
                                        limitsToContentSize: Boolean = false)
                                       (implicit override val scrollContext: ScrollingContext)
	extends ScrollViewFactoryLike[InitializedScrollViewFactory]
		with NonContextualWrapperContainerFactory[ScrollView, ReachComponentLike]
		with FromGenericContextFactory[Any, InitializedContextualScrollViewFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N <: Any](context: N) =
		InitializedContextualScrollViewFactory(parentHierarchy, context, axis, scrollBarMargin, maxOptimalLengths,
			customDrawers, limitsToContentSize)
	
	
	// OTHER	-----------------------------------
	
	override def withAxis(axis: Axis2D) = copy(axis = axis)
	override def withScrollBarMargin(margin: Size) = copy(scrollBarMargin = margin)
	override def withMaxOptimalLengths(maxOptimalLengths: Dimensions[Option[Double]]) =
		copy(maxOptimalLengths = maxOptimalLengths)
	override def withLimitsToContentSize(limits: Boolean) = copy(limitsToContentSize = limits)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) = copy(customDrawers = drawers)
}

case class ContextualScrollViewFactory[N](parentHierarchy: ComponentHierarchy, context: N)
	extends GenericContextualFactory[N, Any, ContextualScrollViewFactory]
		with UninitializedScrollAreaFactory[InitializedContextualScrollViewFactory[N]]
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
                                                     maxOptimalLengths: Dimensions[Option[Double]] = Dimensions(None).empty,
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
	override def withMaxOptimalLengths(maxOptimalLengths: Dimensions[Option[Double]]) =
		copy(maxOptimalLengths = maxOptimalLengths)
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
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = {
		implicit val ds: DrawSettings = StrokeSettings(Hsl(Angle.degrees(math.random() * 360)))
		clipZone match {
			case Some(clip) =>
				clip.overlapWith(bounds).foreach { c =>
					super.paintWith(drawer.clippedToBounds(c), Some(c))
				}
			case None => super.paintWith(drawer.clippedToBounds(bounds), Some(bounds))
		}
	}
}