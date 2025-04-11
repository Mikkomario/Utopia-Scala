package utopia.reach.container.wrapper.scrolling

import utopia.firmament.component.container.single.ScrollViewLike
import utopia.firmament.context.ScrollingContext
import utopia.flow.event.listener.ChangeListener
import utopia.genesis.graphics.Drawer
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy, ReachComponent}
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}
import utopia.reach.container.wrapper.{ContextualWrapperContainerFactory, NonContextualWrapperContainerFactory}

import scala.language.implicitConversions

trait ScrollViewFactoryLike[+Repr] extends ScrollWrapperFactoryLike[ScrollView, Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Axis along which the scrolling occurs
	  */
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
	
	override def apply[C <: ReachComponent, R](content: OpenComponent[C, R]): ComponentWrapResult[ScrollView, C, R] = {
		val view = new ScrollView(hierarchy, content.component, settings, axis)
		applyLengthConstraintsTo(view)
		content.attachTo(view)
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param max Largest allowed optimal length for this view
	  * @return Copy of this factory with the specified maximum applied
	  */
	def withMaxOptimalLength(max: Double) = withMaxOptimalLengthAlong(axis, max)
}

case class ScrollViewFactory(hierarchy: ComponentHierarchy, settings: ScrollingSettings = ScrollingSettings.default)
	extends UninitializedScrollAreaFactory[InitializedScrollViewFactory, ScrollViewFactory]
		with FromGenericContextFactory[Any, ContextualScrollViewFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED  ------------------------
	
	override def withSettings(settings: ScrollingSettings) = copy(settings = settings)
	override def withContext[N <: Any](context: N): ContextualScrollViewFactory[N] =
		ContextualScrollViewFactory(hierarchy, context, settings)
	
	/**
	  * @param scrollContext Scrolling context to use
	  * @return An initialized version of this factory
	  */
	override def withScrollContext(scrollContext: ScrollingContext) =
		InitializedScrollViewFactory(hierarchy, settings)(scrollContext)
}

case class InitializedScrollViewFactory(hierarchy: ComponentHierarchy,
                                        settings: ScrollingSettings = ScrollingSettings.default, axis: Axis2D = Y)
                                       (implicit override val scrollContext: ScrollingContext)
	extends ScrollViewFactoryLike[InitializedScrollViewFactory]
		with NonContextualWrapperContainerFactory[ScrollView, ReachComponent]
		with FromGenericContextFactory[Any, InitializedContextualScrollViewFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withSettings(settings: ScrollingSettings): InitializedScrollViewFactory = copy(settings = settings)
	override def withContext[N <: Any](context: N) =
		InitializedContextualScrollViewFactory(hierarchy, context, settings, axis)
	
	
	// OTHER	-----------------------------------
	
	override def withAxis(axis: Axis2D) = copy(axis = axis)
}

case class ContextualScrollViewFactory[N](hierarchy: ComponentHierarchy, context: N,
                                          settings: ScrollingSettings = ScrollingSettings.default)
	extends GenericContextualFactory[N, Any, ContextualScrollViewFactory]
		with UninitializedScrollAreaFactory[InitializedContextualScrollViewFactory[N], ContextualScrollViewFactory[N]]
		with PartOfComponentHierarchy
{
	// IMPLEMENTED  -----------------------
	
	override def withSettings(settings: ScrollingSettings): ContextualScrollViewFactory[N] = copy(settings = settings)
	override def withContext[N2 <: Any](newContext: N2): ContextualScrollViewFactory[N2] = copy(context = newContext)
	
	/**
	  * @param scrollContext Scrolling context to use
	  * @return An initialized version of this factory
	  */
	override def withScrollContext(scrollContext: ScrollingContext) =
		InitializedContextualScrollViewFactory(hierarchy, context, settings)(scrollContext)
}

case class InitializedContextualScrollViewFactory[N](hierarchy: ComponentHierarchy, context: N,
                                                     settings: ScrollingSettings = ScrollingSettings.default,
                                                     axis: Axis2D = Y)
                                                    (implicit override val scrollContext: ScrollingContext)
	extends ScrollViewFactoryLike[InitializedContextualScrollViewFactory[N]]
		with ContextualWrapperContainerFactory[N, Any, ScrollView, ReachComponent, InitializedContextualScrollViewFactory]
{
	// IMPLEMENTED  --------------------------
	
	override def withSettings(settings: ScrollingSettings): InitializedContextualScrollViewFactory[N] =
		copy(settings = settings)
	override def withContext[N2 <: Any](newContext: N2) =
		copy(context = newContext)
	override def withAxis(axis: Axis2D) = copy(axis = axis)
}

object ScrollView extends Cff[ScrollViewFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ScrollViewFactory(hierarchy)
}
/**
  * A component wrapper which allows scrolling along one axis
  * @author Mikko Hilpinen
  * @since 9.12.2020, v0.1
  */
class ScrollView(override val hierarchy: ComponentHierarchy, override val content: ReachComponent,
                 settings: ScrollingSettings = ScrollingSettings.default, override val axis: Axis2D = Y)
                (implicit context: ScrollingContext)
	extends ConcreteCustomDrawReachComponent with ScrollViewLike[ReachComponent]
{
	// ATTRIBUTES	----------------------------
	
	override val customDrawers = scrollBarDrawerToCustomDrawer(context.scrollBarDrawer) +: settings.customDrawers
	
	
	// INITIAL CODE	----------------------------
	
	// WET WET (see ScrollArea)
	setupMouseHandling(context.actorHandler, context.scrollPerWheelClick)
	sizePointer.addListener(ChangeListener.onAnyChange { updateScrollBarBounds() })
	content.sizePointer.addContinuousAnyChangeListener { updateScrollBarBounds(repaintAfter = true) }
	
	
	// IMPLEMENTED	----------------------------
	
	override def scrollBarMargin: Size = settings.barMargin
	override def limitsToContentSize: Boolean = settings.limitsToContentSize
	override def scrollBarIsInsideContent: Boolean = context.scrollBarIsInsideContent
	override def scrollBarWidth: Double = context.scrollBarWidth
	override def friction: LinearAcceleration = context.scrollFriction
	
	// WET WET (from ScrollArea)
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = {
		clipZone match {
			case Some(clip) =>
				clip.overlapWith(bounds).foreach { c =>
					super.paintWith(drawer.clippedToBounds(c), Some(c))
				}
			case None => super.paintWith(drawer.clippedToBounds(bounds), Some(bounds))
		}
	}
}