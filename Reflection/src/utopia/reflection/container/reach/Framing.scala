package utopia.reflection.container.reach

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, ColorContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, RoundedBackgroundDrawer}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.{CompletableComponentHierarchy, ComponentHierarchy, SeedHierarchyBlock}
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.container.stack.template.layout.FramingLike2
import utopia.reflection.shape.stack.StackInsets

object Framing
{
	/**
	  * Creates a new framing
	  * @param parentHierarchy Parent component hierarchy
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known
	  * @return A new framing
	  */
	def apply(parentHierarchy: ComponentHierarchy, insets: StackInsets, customDrawers: Vector[CustomDrawer] = Vector())
			 (content: ComponentHierarchy => ReachComponentLike) =
		new Framing(parentHierarchy, content(new SeedHierarchyBlock(parentHierarchy.top)), insets, customDrawers)
	
	/**
	  * Creates a new framing with background color
	  * @param parentHierarchy Parent component hierarchy
	  * @param color Background color for this framing
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known.
	  *                Also accepts component creation context.
	  * @param context Component creation context affecting this framing (will be modified for contents)
	  * @return A new framing
	  */
	def withBackground[C2, C1 <: BackgroundSensitive[C2]](parentHierarchy: ComponentHierarchy, color: ComponentColor,
														  insets: StackInsets,
														  moreCustomDrawers: Vector[CustomDrawer] = Vector())
														 (content: (ComponentHierarchy, C2) => ReachComponentLike)
														 (implicit context: C1) =
		apply(parentHierarchy, insets, new BackgroundDrawer(color) +: moreCustomDrawers) { hierarchy =>
			content(hierarchy, context.inContextWithBackground(color))
		}
	
	/**
	  * Creates a new framing with background color
	  * @param parentHierarchy Parent component hierarchy
	  * @param role The role that defines the background color used
	  * @param insets Insets placed around the wrapped component
	  * @param preferredShade Color shade that is preferred when picking framing color (default = standard shade)
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known.
	  *                Also accepts component creation context.
	  * @param context Component creation context affecting this framing (will be modified for contents)
	  * @return A new framing
	  */
	def withBackgroundForRole[C2, C1 <: ColorContextLike with BackgroundSensitive[C2]]
	(parentHierarchy: ComponentHierarchy, role: ColorRole, insets: StackInsets, preferredShade: ColorShade = Standard,
	 moreCustomDrawers: Vector[CustomDrawer] = Vector())(content: (ComponentHierarchy, C2) => ReachComponentLike)
	(implicit context: C1) =
		withBackground[C2, C1](parentHierarchy, context.color(role, preferredShade), insets, moreCustomDrawers)(content)
	
	/**
	  * Creates a new framing that draws a rounded background
	  * @param parentHierarchy Parent component hierarchy
	  * @param color Background color for this framing
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known.
	  *                Also accepts component creation context.
	  * @param context Component creation context affecting this framing (will be modified for contents)
	  * @return A new framing
	  */
	def rounded[C2, C1 <: BackgroundSensitive[C2]](parentHierarchy: ComponentHierarchy, color: ComponentColor,
												   insets: StackInsets,
												   moreCustomDrawers: Vector[CustomDrawer] = Vector())
												  (content: (ComponentHierarchy, C2) => ReachComponentLike)
												  (implicit context: C1) =
	{
		// The rounding amount is based on insets
		val drawer = insets.sides.map { _.optimal }.filter { _ > 0.0 }.minOption match
		{
			case Some(minSideLength) => RoundedBackgroundDrawer.withRadius(color, minSideLength)
			// If the insets default to 0, uses solid background drawing instead
			case None => new BackgroundDrawer(color)
		}
		apply(parentHierarchy, insets, drawer +: moreCustomDrawers) { hierarchy =>
			content(hierarchy, context.inContextWithBackground(color)) }
	}
	
	/**
	  * Creates a new framing that draws a rounded background
	  * @param parentHierarchy Parent component hierarchy
	  * @param role The role that defines the background color used
	  * @param insets Insets placed around the wrapped component
	  * @param preferredShade Color shade that is preferred when picking framing color (default = standard shade)
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known.
	  *                Also accepts component creation context.
	  * @param context Component creation context affecting this framing (will be modified for contents)
	  * @return A new framing
	  */
	def roundedForRole[C2, C1 <: ColorContextLike with BackgroundSensitive[C2]]
	(parentHierarchy: ComponentHierarchy, role: ColorRole, insets: StackInsets, preferredShade: ColorShade = Standard,
	 moreCustomDrawers: Vector[CustomDrawer] = Vector())(content: (ComponentHierarchy, C2) => ReachComponentLike)
	(implicit context: C1) =
		rounded[C2, C1](parentHierarchy, context.color(role, preferredShade), insets, moreCustomDrawers)(content)
}

/**
  * A reach implementation of the framing trait which places insets or margins around a wrapped component
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class Framing(override val parentHierarchy: ComponentHierarchy, override protected val content: ReachComponentLike,
			  override val insets: StackInsets, override val customDrawers: Vector[CustomDrawer] = Vector())
	extends CustomDrawReachComponent with FramingLike2[ReachComponentLike]
{
	// INITIAL CODE	---------------------------
	
	// Completes the content hierarchy, if possible
	content.parentHierarchy match
	{
		case completable: CompletableComponentHierarchy => completable.complete(this)
		case _ => ()
	}
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
}
