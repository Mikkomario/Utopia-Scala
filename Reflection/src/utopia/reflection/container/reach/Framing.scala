package utopia.reflection.container.reach

import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, BaseContextLike, ColorContext, ColorContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, RoundedBackgroundDrawer}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.factory.{BuilderFactory, ComponentFactoryFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reflection.container.stack.template.layout.FramingLike2
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.stack.{StackInsets, StackInsetsConvertible}

object Framing extends ComponentFactoryFactory[FramingFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = FramingFactory(hierarchy)
}

case class FramingFactory(parentHierarchy: ComponentHierarchy) extends BuilderFactory[FramingBuilder]
{
	// IMPLEMENTED	------------------------------
	
	override def build[F](contentFactory: ComponentFactoryFactory[F]) =
		FramingBuilder(this, contentFactory)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new framing builder that uses specified component creation context
	  * @param contentFactory Framing content factory
	  * @param context Framing creation context
	  * @param makeContext A function for producing a content context
	  * @tparam NT Temporary context type between the starting context and the final context
	  * @tparam NC Type of content creation context
	  * @tparam F Type of contextual content factory
	  * @return A new contextual framing builder
	  */
	// TODO: Cannot properly interpret these type parameters
	def buildWithMappedContext[NT, NC <: BaseContextLike, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(contentFactory: ContextInsertableComponentFactoryFactory[_ >: NC, _, F], context: BackgroundSensitive[NT])
	(makeContext: NT => NC) =
		new ContextualFramingBuilder[BackgroundSensitive[NT], NT, NC, F](context, this, contentFactory)(makeContext)
	
	/**
	  * Creates a new framing builder that uses specified component creation context
	  * @param contentFactory Framing content factory
	  * @param context Framing creation context
	  * @tparam NC Type of content creation context
	  * @tparam F Type of contextual content factory
	  * @return A new contextual framing builder
	  */
	def buildWithContext[NC <: BaseContextLike, F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(contentFactory: ContextInsertableComponentFactoryFactory[_ >: NC, _, F], context: BackgroundSensitive[NC]) =
		buildWithMappedContext[NC, NC, F](contentFactory, context) { c => c }
	
	/**
	  * Creates a new framing
	  * @param content A component that is yet to attach to a component hierarchy
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @tparam C Type of wrapped component
	  * @return A new framing and the produced content component
	  */
	def apply[C <: ReachComponentLike, R](content: OpenComponent[C, R], insets: StackInsetsConvertible,
										  customDrawers: Vector[CustomDrawer] = Vector()) =
	{
		val framing = new Framing(parentHierarchy, content, insets.toInsets, customDrawers)
		// Closes the content
		content.attachTo(framing)
	}
}

case class FramingBuilder[+F](framingFactory: FramingFactory, contentFactory: ComponentFactoryFactory[F])
{
	/**
	  * Creates a new framing
	  * @param insets Insets placed around the wrapped component
	  * @param customDrawers Custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy is known
	  * @tparam C Type of wrapped component
	  * @return A new framing and the produced content component
	  */
	def apply[C <: ReachComponentLike, R](insets: StackInsetsConvertible,
										  customDrawers: Vector[CustomDrawer] = Vector())
										 (content: F => ComponentCreationResult[C, R]) =
	{
		val newContent = Open.using(contentFactory)(content)(framingFactory.parentHierarchy.top)
		framingFactory(newContent, insets, customDrawers)
	}
	
	// TODO: Move these to a contextual version of this builder
	/**
	  * Creates a new framing with background color
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
	def withBackground[C <: ReachComponentLike, R, C1 <: BackgroundSensitive[ColorContext]]
	(color: ComponentColor, insets: StackInsetsConvertible, moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (F, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
		apply(insets, BackgroundDrawer(color) +: moreCustomDrawers) { factory =>
			content(factory, context.inContextWithBackground(color))
		}
	
	/**
	  * Creates a new framing with background color
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
	def buildWithBackgroundForRole[C <: ReachComponentLike, R,
		C1 <: ColorContextLike with BackgroundSensitive[ColorContext]]
	(role: ColorRole, insets: StackInsetsConvertible,
	 preferredShade: ColorShade = Standard, moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (F, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
		withBackground[C, R, C1](context.color(role, preferredShade), insets, moreCustomDrawers)(content)
	
	/**
	  * Creates a new framing that draws a rounded background
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
	def rounded[C <: ReachComponentLike, R, C1 <: BackgroundSensitive[ColorContext]]
	(color: ComponentColor, insets: StackInsetsConvertible,
	 moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (F, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
	{
		val activeInsets = insets.toInsets
		// The rounding amount is based on insets
		val drawer = activeInsets.sides.map { _.optimal }.filter { _ > 0.0 }.minOption match
		{
			case Some(minSideLength) => RoundedBackgroundDrawer.withRadius(color, minSideLength)
			// If the insets default to 0, uses solid background drawing instead
			case None => BackgroundDrawer(color)
		}
		apply(activeInsets, drawer +: moreCustomDrawers) { factory =>
			content(factory, context.inContextWithBackground(color)) }
	}
	
	/**
	  * Creates a new framing that draws a rounded background
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
	def roundedForRole[C <: ReachComponentLike, R, C1 <: ColorContextLike with BackgroundSensitive[ColorContext]]
	(role: ColorRole, insets: StackInsetsConvertible,
	 preferredShade: ColorShade = Standard, moreCustomDrawers: Vector[CustomDrawer] = Vector())
	(content: (F, ColorContext) => ComponentCreationResult[C, R])
	(implicit context: C1) =
		rounded[C, R, C1](context.color(role, preferredShade), insets, moreCustomDrawers)(content)
}

object ContextualFramingBuilder
{
	// EXTENSIONS	---------------------------
	
	// An extension for framing builders which have access to a context with color information
	implicit class ColorAwareContextualFramingBuilder[+NP <: BackgroundSensitive[NT] with ColorContextLike,
		+NT, NC <: BaseContextLike, +F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
	(val b: ContextualFramingBuilder[NP, NT, NC, F]) extends AnyVal
	{
		/**
		  * Creates a new framing with background color
		  * @param role Role for this framing's background color
		  * @param insets Insets placed around the wrapped component
		  * @param preferredShade Preferred shade to use with framing background color (default = standard)
		  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
		  * @param content Function for producing the framed content when component hierarchy and component creation
		  *                context are known
		  * @tparam C Type of wrapped component
		  * @tparam R Type of additional component creation result
		  * @return A new framing and the produced content component
		  */
		def withBackgroundForRole[C <: ReachComponentLike, R](role: ColorRole, insets: StackInsetsConvertible,
															  preferredShade: ColorShade = Standard,
															  moreCustomDrawers: Vector[CustomDrawer] = Vector())
															 (content: F[NC] => ComponentCreationResult[C, R]) =
			b.withBackground(b.context.color(role, preferredShade), insets, moreCustomDrawers)(content)
		
		/**
		  * Creates a new framing with background color and rounded corners
		  * @param role Role for this framing's background color
		  * @param insets Insets placed around the wrapped component
		  * @param preferredShade Preferred shade to use with framing background color (default = standard)
		  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
		  * @param content Function for producing the framed content when component hierarchy and component creation
		  *                context are known
		  * @tparam C Type of wrapped component
		  * @tparam R Type of additional component creation result
		  * @return A new framing and the produced content component
		  */
		def roundedForRole[C <: ReachComponentLike, R](role: ColorRole, insets: StackInsetsConvertible,
													   preferredShade: ColorShade = Standard,
													   moreCustomDrawers: Vector[CustomDrawer] = Vector())
													  (content: F[NC] => ComponentCreationResult[C, R]) =
			b.rounded(b.context.color(role, preferredShade), insets, moreCustomDrawers)(content)
	}
}

class ContextualFramingBuilder[+NP <: BackgroundSensitive[NT], +NT, NC <: BaseContextLike,
	+F[X <: NC] <: ContextualComponentFactory[X, _ >: NC, F]]
(val context: NP, framingFactory: FramingFactory,
 contentFactory: ContextInsertableComponentFactoryFactory[_ >: NC, _, F])(makeContext: NT => NC)
{
	// IMPLICIT	-------------------------------
	
	private implicit def canvas: ReachCanvas = framingFactory.parentHierarchy.top
	
	
	// OTHER	-------------------------------
	
	/**
	  * Creates a new framing with background color
	  * @param color Background color for this framing
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy and component creation
	  *                context are known
	  * @tparam C Type of wrapped component
	  * @tparam R Type of additional component creation result
	  * @return A new framing and the produced content component
	  */
	def withBackground[C <: ReachComponentLike, R](color: ComponentColor, insets: StackInsetsConvertible,
												   moreCustomDrawers: Vector[CustomDrawer] = Vector())
												  (content: F[NC] => ComponentCreationResult[C, R]) =
		apply(color, insets, BackgroundDrawer(color) +: moreCustomDrawers)(content)
	
	/**
	  * Creates a new framing with background color and rounding at the four corners
	  * @param color Background color for this framing
	  * @param insets Insets placed around the wrapped component
	  * @param moreCustomDrawers Additional custom drawers applied to this framing (default = empty)
	  * @param content Function for producing the framed content when component hierarchy and component creation
	  *                context are known
	  * @tparam C Type of wrapped component
	  * @tparam R Type of additional component creation result
	  * @return A new framing and the produced content component
	  */
	def rounded[C <: ReachComponentLike, R](color: ComponentColor, insets: StackInsetsConvertible,
											moreCustomDrawers: Vector[CustomDrawer] = Vector())
										   (content: F[NC] => ComponentCreationResult[C, R]) =
	{
		val activeInsets = insets.toInsets
		// The rounding amount is based on insets
		val drawer = activeInsets.sides.map { _.optimal }.filter { _ > 0.0 }.minOption match
		{
			case Some(minSideLength) => RoundedBackgroundDrawer.withRadius(color, minSideLength)
			// If the insets default to 0, uses solid background drawing instead
			case None => BackgroundDrawer(color)
		}
		apply(color, activeInsets, drawer +: moreCustomDrawers)(content)
	}
	
	private def apply[C <: ReachComponentLike, R](background: ComponentColor, insets: StackInsetsConvertible,
												  customDrawers: Vector[CustomDrawer] = Vector())
												 (content: F[NC] => ComponentCreationResult[C, R]) =
	{
		val contentContext: NC = makeContext(context.inContextWithBackground(background))
		val newContent = Open.withContext(contentFactory, contentContext)(content)
		framingFactory(newContent, insets, customDrawers)
	}
}

/**
  * A reach implementation of the framing trait which places insets or margins around a wrapped component
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class Framing(override val parentHierarchy: ComponentHierarchy, override val content: ReachComponentLike,
			  override val insets: StackInsets, override val customDrawers: Vector[CustomDrawer] = Vector())
	extends CustomDrawReachComponent with FramingLike2[ReachComponentLike]