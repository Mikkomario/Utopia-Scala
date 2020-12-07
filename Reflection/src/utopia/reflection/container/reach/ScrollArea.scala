package utopia.reflection.container.reach

import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.LinearAcceleration
import utopia.genesis.shape.shape2D.{Bounds, Size}
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, BaseContextLike, ColorContextLike, ScrollingContextLike}
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.reflection.component.reach.factory.{ComponentFactoryFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.container.stack.template.scrolling.ScrollAreaLike2
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.stack.StackLengthLimit
import utopia.reflection.util.ComponentCreationDefaults

import scala.collection.immutable.HashMap

object ScrollArea
{
	// TODO: Continue
}

class ScrollAreaFactory(val parentHierarchy: ComponentHierarchy)
{
	def build[F](contentFactory: ComponentFactoryFactory[F]) = new ScrollAreaBuilder[F](this, contentFactory)
	
	def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R], scrollBarMargin: Size = Size.zero,
			  lengthLimits: Map[Axis2D, StackLengthLimit] = HashMap(), customDrawers: Vector[CustomDrawer] = Vector(),
			  limitsToContentSize: Boolean = false)(implicit context: ScrollingContextLike) =
	{
		val area = new ScrollArea(parentHierarchy, content.component, context.actorHandler, context.scrollBarDrawer,
			context.scrollBarWidth, scrollBarMargin, context.scrollPerWheelClick, context.scrollFriction, lengthLimits,
			customDrawers, limitsToContentSize, context.scrollBarIsInsideContent)
		content.attachTo(area)
	}
}

class ScrollAreaBuilder[+F](factory: ScrollAreaFactory, contentFactory: ComponentFactoryFactory[F])
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	def apply[C <: ReachComponentLike, R](scrollbarMargin: Size = Size.zero,
										  lengthLimits: Map[Axis2D, StackLengthLimit] = HashMap(),
										  customDrawers: Vector[CustomDrawer] = Vector(),
										  limitsToContentSize: Boolean = false)
										 (fill: F => ComponentCreationResult[C, R])
										 (implicit context: ScrollingContextLike) =
	{
		val content = Open.using(contentFactory)(fill)
		factory(content, scrollbarMargin, lengthLimits, customDrawers, limitsToContentSize)
	}
}

object ContextualBackgroundScrollAreaBuilder
{
	// EXTENSIONS	---------------------------
	
	// An extension for scroll area builders which have access to a context with color information
	implicit class ColorAwareContextualScrollAreaBuilder[+NP <: BackgroundSensitive[NT] with ColorContextLike,
		+NT, NC, +F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(val b: ContextualBackgroundScrollAreaBuilder[NP, NT, NC, F]) extends AnyVal
	{
		def withBackgroundForRole[C <: ReachComponentLike, R](role: ColorRole, preferredShade: ColorShade = Standard,
															  scrollbarMargin: Size = Size.zero,
															  lengthLimits: Map[Axis2D, StackLengthLimit] = HashMap(),
															  customDrawers: Vector[CustomDrawer] = Vector(),
															  limitsToContentSize: Boolean = false)
															 (content: F[NC] => ComponentCreationResult[C, R])
															 (implicit scrollContext: ScrollingContextLike) =
			b.withBackground(b.context.color(role, preferredShade), scrollbarMargin, lengthLimits, customDrawers,
				limitsToContentSize)(content)
	}
}

class ContextualBackgroundScrollAreaBuilder[+NP <: BackgroundSensitive[NT], +NT, NC,
	+F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
(factory: ScrollAreaFactory, private val context: NP,
 contentFactory: ContextInsertableComponentFactoryFactory[_ >: NC, _, F])
(makeContext: NT => NC)
{
	private implicit def canvas: ReachCanvas = factory.parentHierarchy.top
	
	def withBackground[C <: ReachComponentLike, R](backgroundColor: ComponentColor, scrollbarMargin: Size = Size.zero,
												   lengthLimits: Map[Axis2D, StackLengthLimit] = HashMap(),
												   customDrawers: Vector[CustomDrawer] = Vector(),
												   limitsToContentSize: Boolean = false)
												  (fill: F[NC] => ComponentCreationResult[C, R])
												  (implicit scrollContext: ScrollingContextLike) =
	{
		val contentContext: NC = makeContext(context.inContextWithBackground(backgroundColor))
		val content = Open.withContext(contentFactory, contentContext)(fill)
		factory(content, scrollbarMargin, lengthLimits, customDrawers :+ BackgroundDrawer(backgroundColor),
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
				 override val lengthLimits: Map[Axis2D, StackLengthLimit] = HashMap(),
				 additionalDrawers: Vector[CustomDrawer] = Vector(),
				 override val limitsToContentSize: Boolean = false,
				 override val scrollBarIsInsideContent: Boolean = true)
	extends CustomDrawReachComponent with ScrollAreaLike2[ReachComponentLike]
{
	// ATTRIBUTES	----------------------------
	
	override val customDrawers = scrollBarDrawerToCustomDrawer(barDrawer) +: additionalDrawers
	
	
	// INITIAL CODE	----------------------------
	
	setupMouseHandling(actorHandler, scrollPerWheelClick)
	
	
	// IMPLEMENTED	----------------------------
	
	override def axes = Axis2D.values
	
	override def repaint(bounds: Bounds) = parentHierarchy.repaint(bounds + position)
}
