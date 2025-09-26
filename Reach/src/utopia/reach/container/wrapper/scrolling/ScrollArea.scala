package utopia.reach.container.wrapper.scrolling

import utopia.firmament.component.container.single.ScrollAreaLike
import utopia.firmament.component.stack.Constrainable
import utopia.firmament.context.ScrollingContext
import utopia.firmament.model.stack.modifier.MaxOptimalLengthModifier
import utopia.flow.event.listener.ChangeListener
import utopia.genesis.graphics.Drawer
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.ComponentFactories.CF
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy, ReachComponent}
import utopia.reach.component.wrapper.{ContainerCreation, Open}
import utopia.reach.container.wrapper.{ContextualWrapperContainerFactory, NonContextualWrapperContainerFactory, WrapperContainerFactory}

import scala.language.implicitConversions

/**
  * Common trait for container factories that yield ScrollAreas or some of their subclasses
  * @tparam Container The type of container yielded by this factory
  * @tparam Repr This factory type
  */
trait ScrollWrapperFactoryLike[+Container, +Repr]
	extends WrapperContainerFactory[Container, ReachComponent] with ScrollingSettingsWrapper[Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Context used for specifying certain scrolling functionality
	  */
	implicit def scrollContext: ScrollingContext
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param long Margin to place along the long edge of this scroll view
	  * @param ends Margin to place at each end of the scroll bar
	  * @return Copy of this factory with the specified margins
	  */
	@deprecated("Please use .withBarMargin(Double, Double) instead", "v1.6")
	def withScrollBarMargin(long: Double, ends: Double): Repr = withScrollBarMargin(Size(long, ends))
	@deprecated("Please use .mapBarMargin(...) instead", "v1.6")
	def mapScrollBarMargin(f: Size => Size) = mapBarMargin(f)
	
	/**
	  * @param margin Margin to place between the long (scrolling) edge of this view and the painted scroll bar
	  * @return Copy of this factory with the specified margin in place
	  */
	@deprecated("Please use .withLongEdgeBarMargin(Double) instead", "v1.6")
	def withLongEdgeScrollBarMargin(margin: Double) = mapScrollBarMargin { _.withX(margin) }
	/**
	  * @param margin Margin to place at each scroll bar end
	  * @return Copy of this factory with the specified margins
	  */
	@deprecated("Please use .withBarCap(Double) instead", "v1.6")
	def withEndScrollBarMargins(margin: Double) = mapScrollBarMargin { _.withY(margin) }
	/**
	  * @param margin Margins placed around the scroll bars (wider edge margin as X and thinner edge margins as Y)
	  *               (default = 0x0)
	  * @return Copy of this factory with the specified margins
	  */
	@deprecated("Please use .withBarMargin(Size) instead", "v1.6")
	def withScrollBarMargin(margin: Size): Repr = withBarMargin(margin)
	
	/**
	  * Applies the 'maxOptimalLengths' as constraints
	  * @param c Component for which the constraints are added
	  */
	protected def applyLengthConstraintsTo(c: Constrainable) = {
		maxOptimalLengths.components.foreach { maxLength =>
			maxLength.value.foreach { max =>
				maxLength.axis match {
					case axis2D: Axis2D => c.addConstraintOver(axis2D)(MaxOptimalLengthModifier(max))
					case _ => ()
				}
			}
		}
	}
}

trait ScrollAreaFactoryLike[+Repr] extends ScrollWrapperFactoryLike[ScrollArea, Repr]
{
	// IMPLEMENTED  ---------------------
	
	override def apply[C <: ReachComponent, R](content: Open[C, R]): ContainerCreation[ScrollArea, C, R] = {
		val area = new ScrollArea(hierarchy, content.component, settings)
		applyLengthConstraintsTo(area)
		content.attachTo(area)
	}
}

object UninitializedScrollAreaFactory
{
	implicit def autoInitialize[F](f: UninitializedScrollAreaFactory[F, _])(implicit c: ScrollingContext): F =
		f.initialized
}
trait UninitializedScrollAreaFactory[+Initialized, +Repr] extends ScrollingSettingsWrapper[Repr]
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

case class ScrollAreaFactory(hierarchy: ComponentHierarchy, settings: ScrollingSettings = ScrollingSettings.default)
	extends UninitializedScrollAreaFactory[InitializedScrollAreaFactory, ScrollAreaFactory]
		with FromGenericContextFactory[Any, ContextualScrollAreaFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED	----------------------------------
	
	override def withScrollContext(scrollContext: ScrollingContext): InitializedScrollAreaFactory =
		InitializedScrollAreaFactory(hierarchy, settings)(scrollContext)
	
	override def withContext[N <: Any](context: N) =
		ContextualScrollAreaFactory(hierarchy, context, settings)
	override def withSettings(settings: ScrollingSettings) = copy(settings = settings)
}

case class InitializedScrollAreaFactory(hierarchy: ComponentHierarchy, settings: ScrollingSettings)
                                       (implicit override val scrollContext: ScrollingContext)
	extends ScrollAreaFactoryLike[InitializedScrollAreaFactory]
		with NonContextualWrapperContainerFactory[ScrollArea, ReachComponent]
		with FromGenericContextFactory[Any, InitializedContextualScrollAreaFactory]
{
	override def withContext[N <: Any](context: N): InitializedContextualScrollAreaFactory[N] =
		InitializedContextualScrollAreaFactory(hierarchy, context, settings)
	override def withSettings(settings: ScrollingSettings): InitializedScrollAreaFactory = copy(settings = settings)
}

case class ContextualScrollAreaFactory[N](hierarchy: ComponentHierarchy, context: N,
                                          settings: ScrollingSettings = ScrollingSettings.default)
	extends GenericContextualFactory[N, Any, ContextualScrollAreaFactory]
		with UninitializedScrollAreaFactory[InitializedContextualScrollAreaFactory[N], ContextualScrollAreaFactory[N]]
		with PartOfComponentHierarchy
{
	// IMPLEMENTED	--------------------------
	
	override def withScrollContext(scrollContext: ScrollingContext): InitializedContextualScrollAreaFactory[N] =
		InitializedContextualScrollAreaFactory(hierarchy, context, settings)(scrollContext)
	
	override def withContext[N2](newContext: N2) = copy(context = newContext)
	override def withSettings(settings: ScrollingSettings) = copy(settings = settings)
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return A version of this factory which doesn't utilize component creation context
	  */
	@deprecated("Deprecated for removal", "v1.1")
	def withoutContext = ScrollAreaFactory(hierarchy, settings)
}

case class InitializedContextualScrollAreaFactory[N](hierarchy: ComponentHierarchy, context: N,
                                                     settings: ScrollingSettings = ScrollingSettings.default)
                                                    (implicit override val scrollContext: ScrollingContext)
	extends ScrollAreaFactoryLike[InitializedContextualScrollAreaFactory[N]]
		with ContextualWrapperContainerFactory[N, Any, ScrollArea, ReachComponent, InitializedContextualScrollAreaFactory]
{
	override def withContext[N2](newContext: N2): InitializedContextualScrollAreaFactory[N2] =
		copy(context = newContext)
	override def withSettings(settings: ScrollingSettings): InitializedContextualScrollAreaFactory[N] =
		copy(settings = settings)
}

object ScrollArea extends CF[ScrollAreaFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ScrollAreaFactory(hierarchy)
}
/**
  * A component wrapper which allows content scrolling
  * @author Mikko Hilpinen
  * @since 7.12.2020, v0.1
  */
class ScrollArea(override val hierarchy: ComponentHierarchy, override val content: ReachComponent,
                 settings: ScrollingSettings = ScrollingSettings.default)
                (implicit context: ScrollingContext)
	extends ConcreteCustomDrawReachComponent with ScrollAreaLike[ReachComponent]
{
	// ATTRIBUTES	----------------------------
	
	override val customDrawers = scrollBarDrawerToCustomDrawer(context.scrollBarDrawer) +: settings.customDrawers
	
	
	// INITIAL CODE	----------------------------
	
	setupMouseHandling(context.actorHandler, context.scrollPerWheelClick)
	sizePointer.addListener(ChangeListener.onAnyChange { updateScrollBarBounds() })
	content.boundsPointer.addContinuousAnyChangeListener { updateScrollBarBounds(repaintAfter = true) }
	
	
	// IMPLEMENTED	----------------------------
	
	override def axes = Axis2D.values
	override def scrollBarMargin: Size = settings.barMargin
	override def limitsToContentSize: Boolean = settings.limitsToContentSize
	override def scrollBarIsInsideContent: Boolean = context.scrollBarIsInsideContent
	override def scrollBarWidth: Double = context.scrollBarWidth
	override def friction: LinearAcceleration = context.scrollFriction
	
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = {
		clipZone match {
			case Some(clip) =>
				clip.overlapWith(bounds).foreach { c => super.paintWith(drawer.clippedToBounds(c), Some(c))}
			case None => super.paintWith(drawer.clippedToBounds(bounds), Some(bounds))
		}
	}
}
