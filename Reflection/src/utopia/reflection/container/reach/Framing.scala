package utopia.reflection.container.reach

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, ColorContext, ColorContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, RoundedBackgroundDrawer}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, OpenComponent}
import utopia.reflection.container.stack.template.layout.FramingLike2
import utopia.reflection.shape.stack.{StackInsets, StackInsetsConvertible}

object Framing
{
	/**
	  * Creates a new framing
	  * @param parentHierarchy Parent component hierarchy
	  * @param content A component that is yet to attach to a component hierarchy
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @tparam C Type of wrapped component
	  * @return A new framing and the produced content component
	  */
	def apply[C <: ReachComponentLike, R](parentHierarchy: ComponentHierarchy, content: OpenComponent[C, R],
										  insets: StackInsetsConvertible, customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val framing = new Framing(parentHierarchy, content, insets.toInsets, customDrawers)
		// Closes the content
		content.attachTo(framing)
	}
	
	/**
	  * Creates a new framing
	  * @param parentHierarchy Parent component hierarchy
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known
	  * @tparam C Type of wrapped component
	  * @return A new framing and the produced content component
	  */
	def build[C <: ReachComponentLike, R](parentHierarchy: ComponentHierarchy, insets: StackInsetsConvertible,
									   customDrawers: Vector[CustomDrawer] = Vector())
									  (content: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		val newContent = OpenComponent(content)(parentHierarchy.top)
		apply(parentHierarchy, newContent, insets, customDrawers)
	}
	
	/**
	  * Creates a new framing with background color
	  * @param parentHierarchy Parent component hierarchy
	  * @param color Background color for this framing
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known.
	  *                Also accepts component creation context.
	  * @param context Component creation context affecting this framing (will be modified for contents)
	  * @tparam C Type of wrapped component
	  * @tparam C1 Type of component creation context that is used to produce the altered creation context
	  * @return A new framing and the produced content component
	  */
	def buildWithBackground[C <: ReachComponentLike, R, C1 <: BackgroundSensitive[ColorContext]]
	(parentHierarchy: ComponentHierarchy, color: ComponentColor, insets: StackInsetsConvertible,
	 moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (ComponentHierarchy, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
		build(parentHierarchy, insets, new BackgroundDrawer(color) +: moreCustomDrawers) { hierarchy =>
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
	  * @tparam C Type of wrapped component
	  * @tparam C1 Type of component creation context that is used to produce the altered creation context
	  * @return A new framing and the produced content component
	  */
	def buildWithBackgroundForRole[C <: ReachComponentLike, R, C1 <: ColorContextLike with BackgroundSensitive[ColorContext]]
	(parentHierarchy: ComponentHierarchy, role: ColorRole, insets: StackInsetsConvertible,
	 preferredShade: ColorShade = Standard, moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (ComponentHierarchy, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
		buildWithBackground[C, R, C1](parentHierarchy, context.color(role, preferredShade), insets, moreCustomDrawers)(content)
	
	/**
	  * Creates a new framing that draws a rounded background
	  * @param parentHierarchy Parent component hierarchy
	  * @param color Background color for this framing
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known.
	  *                Also accepts component creation context.
	  * @param context Component creation context affecting this framing (will be modified for contents)
	  * @tparam C Type of wrapped component
	  * @tparam C1 Type of component creation context that is used to produce the altered creation context
	  * @return A new framing and the produced content component
	  */
	def buildRounded[C <: ReachComponentLike, R, C1 <: BackgroundSensitive[ColorContext]]
	(parentHierarchy: ComponentHierarchy, color: ComponentColor, insets: StackInsetsConvertible,
	 moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (ComponentHierarchy, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
	{
		val activeInsets = insets.toInsets
		// The rounding amount is based on insets
		val drawer = activeInsets.sides.map { _.optimal }.filter { _ > 0.0 }.minOption match
		{
			case Some(minSideLength) => RoundedBackgroundDrawer.withRadius(color, minSideLength)
			// If the insets default to 0, uses solid background drawing instead
			case None => new BackgroundDrawer(color)
		}
		build(parentHierarchy, activeInsets, drawer +: moreCustomDrawers) { hierarchy =>
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
	  * @tparam C Type of wrapped component
	  * @tparam C1 Type of component creation context that is used to produce the altered creation context
	  * @return A new framing and the produced content component
	  */
	def buildRoundedForRole[C <: ReachComponentLike, R, C1 <: ColorContextLike with BackgroundSensitive[ColorContext]]
	(parentHierarchy: ComponentHierarchy, role: ColorRole, insets: StackInsetsConvertible,
	 preferredShade: ColorShade = Standard, moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (ComponentHierarchy, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
		buildRounded[C, R, C1](parentHierarchy, context.color(role, preferredShade), insets, moreCustomDrawers)(content)
}

/**
  * A reach implementation of the framing trait which places insets or margins around a wrapped component
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class Framing(override val parentHierarchy: ComponentHierarchy, override val content: ReachComponentLike,
			  override val insets: StackInsets, override val customDrawers: Vector[CustomDrawer] = Vector())
	extends CustomDrawReachComponent with FramingLike2[ReachComponentLike]
{
	// IMPLEMENTED	---------------------------
	
	override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
}
