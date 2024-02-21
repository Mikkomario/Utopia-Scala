package utopia.reach.container.wrapper.scrolling

import utopia.firmament.component.container.single.ScrollAreaLike
import utopia.firmament.component.stack.Constrainable
import utopia.firmament.context.{ComponentCreationDefaults, ScrollingContext}
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.firmament.model.stack.modifier.MaxOptimalLengthModifier
import utopia.flow.event.listener.ChangeListener
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.action.ActorHandler
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
import utopia.reach.container.wrapper.{ContextualWrapperContainerFactory, NonContextualWrapperContainerFactory, WrapperContainerFactory}

import scala.language.implicitConversions

object ScrollArea extends Cff[ScrollAreaFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ScrollAreaFactory(hierarchy)
}

/**
  * Common trait for container factories that yield ScrollAreas or some of their sub-classes
  * @tparam Container The type of container yielded by this factory
  * @tparam Repr This factory type
  */
trait ScrollWrapperFactoryLike[+Container, +Repr]
	extends WrapperContainerFactory[Container, ReachComponentLike] with CustomDrawableFactory[Repr]
{
	// ABSTRACT ---------------------------
	
	def scrollContext: ScrollingContext
	def scrollBarMargin: Size
	def maxOptimalLengths: Dimensions[Option[Double]]
	def limitsToContentSize: Boolean
	
	/**
	  * @param margin Margins placed around the scroll bars (wider edge margin as X and thinner edge margins as Y)
	  *               (default = 0x0)
	  * @return Copy of this factory with the specified margins
	  */
	def withScrollBarMargin(margin: Size): Repr
	/**
	  * @param maxLengths Maximum allowed optimal lengths for this scroll area. None where not applicable.
	  * @return A copy of this factory with the specified limits
	  */
	def withMaxOptimalLengths(maxLengths: Dimensions[Option[Double]]): Repr
	/**
	  * @param limits Whether the size of this view should be limited to the length of the content
	  * @return Copy of this factory with the specified setting in place
	  */
	def withLimitsToContentSize(limits: Boolean): Repr
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Copy of this factory that limits the scroll view length to content length
	  */
	def limitedToContentSize = withLimitsToContentSize(limits = true)
	
	
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
	
	def mapMaxOptimalLengths(f: Dimensions[Option[Double]] => Dimensions[Option[Double]]) =
		withMaxOptimalLengths(f(maxOptimalLengths))
	/**
	  * @param maxOptimalWidth Largest allowed optimal width
	  * @return Copy of this factory that specifies maximum optimal width
	  */
	def withMaxOptimalWidth(maxOptimalWidth: Double) = mapMaxOptimalLengths { _.withX(Some(maxOptimalWidth)) }
	/**
	  * @param maxOptimalHeight Largest allowed optimal height
	  * @return Copy of this factory that specifies maximum optimal height
	  */
	def withMaxOptimalHeight(maxOptimalHeight: Double) = mapMaxOptimalLengths { _.withY(Some(maxOptimalHeight)) }
	/**
	  * @param maxOptimalSize Maximum allowed optimal size
	  * @return Copy of this factory that limits maximum optimal size
	  */
	def withMaxOptimalSize(maxOptimalSize: Size) =
		withMaxOptimalLengths(maxOptimalSize.dimensions.mapWithZero[Option[Double]](None) { Some(_) })
	
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
	
	override def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R]): ComponentWrapResult[ScrollArea, C, R] = {
		val context = scrollContext
		val area = new ScrollArea(parentHierarchy, content.component, context.actorHandler, context.scrollBarDrawer,
			context.scrollBarWidth, scrollBarMargin, context.scrollPerWheelClick, context.scrollFriction,
			customDrawers, limitsToContentSize, context.scrollBarIsInsideContent)
		applyLengthConstraintsTo(area)
		content.attachTo(area)
	}
}

object UninitializedScrollAreaFactory
{
	implicit def autoInitialize[F](f: UninitializedScrollAreaFactory[F])(implicit c: ScrollingContext): F =
		f.initialized
}

trait UninitializedScrollAreaFactory[+Initialized]
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

case class ScrollAreaFactory(parentHierarchy: ComponentHierarchy)
	extends UninitializedScrollAreaFactory[InitializedScrollAreaFactory]
		with FromGenericContextFactory[Any, ContextualScrollAreaFactory]
{
	// IMPLEMENTED	----------------------------------
	
	override def withScrollContext(scrollContext: ScrollingContext): InitializedScrollAreaFactory =
		InitializedScrollAreaFactory(parentHierarchy)(scrollContext)
	
	override def withContext[N <: Any](context: N) =
		ContextualScrollAreaFactory(parentHierarchy, context)
}

case class InitializedScrollAreaFactory(parentHierarchy: ComponentHierarchy, scrollBarMargin: Size = Size.zero,
                                        maxOptimalLengths: Dimensions[Option[Double]] = Dimensions[Option[Double]](None).empty,
                                        customDrawers: Vector[CustomDrawer] = Vector(),
                                        limitsToContentSize: Boolean = false)
                                       (implicit override val scrollContext: ScrollingContext)
	extends ScrollAreaFactoryLike[InitializedScrollAreaFactory]
		with NonContextualWrapperContainerFactory[ScrollArea, ReachComponentLike]
		with FromGenericContextFactory[Any, InitializedContextualScrollAreaFactory]
{
	override def withContext[N <: Any](context: N): InitializedContextualScrollAreaFactory[N] =
		InitializedContextualScrollAreaFactory(parentHierarchy, context, scrollBarMargin, maxOptimalLengths,
			customDrawers, limitsToContentSize)
	
	override def withScrollBarMargin(margin: Size): InitializedScrollAreaFactory = copy(scrollBarMargin = margin)
	override def withMaxOptimalLengths(maxLengths: Dimensions[Option[Double]]): InitializedScrollAreaFactory =
		copy(maxOptimalLengths = maxLengths)
	override def withLimitsToContentSize(limits: Boolean): InitializedScrollAreaFactory =
		copy(limitsToContentSize = limits)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): InitializedScrollAreaFactory =
		copy(customDrawers = drawers)
}

case class ContextualScrollAreaFactory[N](parentHierarchy: ComponentHierarchy, context: N)
	extends GenericContextualFactory[N, Any, ContextualScrollAreaFactory]
		with UninitializedScrollAreaFactory[InitializedContextualScrollAreaFactory[N]]
{
	// IMPLEMENTED	--------------------------
	
	override def withScrollContext(scrollContext: ScrollingContext): InitializedContextualScrollAreaFactory[N] =
		InitializedContextualScrollAreaFactory(parentHierarchy, context)(scrollContext)
	
	override def withContext[N2 <: Any](newContext: N2) = copy(context = newContext)
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return A version of this factory which doesn't utilize component creation context
	  */
	@deprecated("Deprecated for removal", "v1.1")
	def withoutContext = ScrollAreaFactory(parentHierarchy)
}

case class InitializedContextualScrollAreaFactory[N](parentHierarchy: ComponentHierarchy, context: N,
                                                     scrollBarMargin: Size = Size.zero,
                                                     maxOptimalLengths: Dimensions[Option[Double]] = Dimensions[Option[Double]](None).empty,
                                                     customDrawers: Vector[CustomDrawer] = Vector(),
                                                     limitsToContentSize: Boolean = false)
                                                    (implicit override val scrollContext: ScrollingContext)
	extends ScrollAreaFactoryLike[InitializedContextualScrollAreaFactory[N]]
		with ContextualWrapperContainerFactory[N, Any, ScrollArea, ReachComponentLike, InitializedContextualScrollAreaFactory]
{
	override def withContext[N2 <: Any](newContext: N2): InitializedContextualScrollAreaFactory[N2] =
		copy(context = newContext)
	
	override def withScrollBarMargin(margin: Size) = copy(scrollBarMargin = margin)
	override def withMaxOptimalLengths(maxLengths: Dimensions[Option[Double]]) =
		copy(maxOptimalLengths = maxLengths)
	override def withLimitsToContentSize(limits: Boolean) =
		copy(limitsToContentSize = limits)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		copy(customDrawers = drawers)
}

/**
  * A component wrapper which allows content scrolling
  * @author Mikko Hilpinen
  * @since 7.12.2020, v0.1
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
	extends CustomDrawReachComponent with ScrollAreaLike[ReachComponentLike]
{
	// ATTRIBUTES	----------------------------
	
	override val customDrawers = scrollBarDrawerToCustomDrawer(barDrawer) +: additionalDrawers
	
	
	// INITIAL CODE	----------------------------
	
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	sizePointer.addListener(ChangeListener.onAnyChange { updateScrollBarBounds() })
	content.boundsPointer.addContinuousAnyChangeListener { updateScrollBarBounds(repaintAfter = true) }
	
	
	// IMPLEMENTED	----------------------------
	
	override def axes = Axis2D.values
	
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = {
		clipZone match {
			case Some(clip) =>
				clip.overlapWith(bounds).foreach { c => super.paintWith(drawer.clippedToBounds(c), Some(c))}
			case None => super.paintWith(drawer.clippedToBounds(bounds), Some(bounds))
		}
	}
}
