package utopia.reach.container.wrapper.scrolling

import utopia.firmament.component.container.single.ScrollAreaLike
import utopia.firmament.context.{ComponentCreationDefaults, ScrollingContext}
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.firmament.model.stack.modifier.MaxOptimalSizeModifier
import utopia.flow.event.listener.ChangeListener
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.{Bounds, Size}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.{BuilderFactory, ComponentFactoryFactory, FromGenericContextFactory, GenericContextualFactory, SimpleFilledBuilderFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas2

object ScrollArea extends Cff[ScrollAreaFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ScrollAreaFactory(hierarchy)
}

class ScrollAreaFactory(val parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[Any, ContextualScrollAreaFactory] with BuilderFactory[ScrollAreaBuilder]
		with SimpleFilledBuilderFactory[ContextualFilledScrollAreaBuilder]
{
	// IMPLEMENTED	----------------------------------
	
	override def withContext[N <: Any](context: N) =
		ContextualScrollAreaFactory(this, context)
	
	override def build[F](contentFactory: ComponentFactoryFactory[F]) =
		new ScrollAreaBuilder[F](this, contentFactory)
	
	protected def makeBuilder[NC, F](background: Color, contentContext: NC, contentFactory: Ccff[NC, F]) =
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
										  limitsToContentSize: Boolean = false)(implicit context: ScrollingContext) =
	{
		val area = new ScrollArea(parentHierarchy, content.component, context.actorHandler, context.scrollBarDrawer,
			context.scrollBarWidth, scrollBarMargin, context.scrollPerWheelClick, context.scrollFriction,
			customDrawers, limitsToContentSize, context.scrollBarIsInsideContent)
		maxOptimalSize.foreach { maxOptimal => area.addConstraint(MaxOptimalSizeModifier(maxOptimal)) }
		content.attachTo(area)
	}
}

case class ContextualScrollAreaFactory[N](factory: ScrollAreaFactory, context: N)
	extends GenericContextualFactory[N, Any, ContextualScrollAreaFactory]
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
	def build[F](contentFactory: Ccff[N, F]) = new ContextualScrollAreaBuilder[N, F](factory, context, contentFactory)
}

class ScrollAreaBuilder[+F](factory: ScrollAreaFactory, contentFactory: ComponentFactoryFactory[F])
{
	private implicit def canvas: ReachCanvas2 = factory.parentHierarchy.top
	
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
										 (implicit context: ScrollingContext) =
	{
		val content = Open.using(contentFactory)(fill)
		factory(content, scrollBarMargin, maxOptimalSize, customDrawers, limitsToContentSize)
	}
}

class ContextualScrollAreaBuilder[N, +F](factory: ScrollAreaFactory, context: N, contentFactory: Ccff[N, F])
{
	private implicit def canvas: ReachCanvas2 = factory.parentHierarchy.top
	
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
										 (fill: F => ComponentCreationResult[C, R])
										 (implicit scrollingContext: ScrollingContext) =
	{
		val content = Open.withContext(context)(contentFactory)(fill)
		factory(content, scrollBarMargin, maxOptimalSize, customDrawers, limitsToContentSize)
	}
}

class ContextualFilledScrollAreaBuilder[NC, +F](factory: ScrollAreaFactory, background: Color, contentContext: NC,
                                                contentFactory: Ccff[NC, F])
{
	private implicit def canvas: ReachCanvas2 = factory.parentHierarchy.top
	
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
										 (fill: F => ComponentCreationResult[C, R])
										 (implicit scrollContext: ScrollingContext) =
	{
		val content = Open.withContext(contentContext)(contentFactory)(fill)
		factory(content, scrollBarMargin, maxOptimalSize, customDrawers :+ BackgroundDrawer(background),
			limitsToContentSize)
	}
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
			case Some(clip) => clip.overlapWith(bounds).foreach { c => super.paintWith(drawer.clippedToBounds(c), Some(c)) }
			case None => super.paintWith(drawer.clippedToBounds(bounds), Some(bounds))
		}
	}
}
