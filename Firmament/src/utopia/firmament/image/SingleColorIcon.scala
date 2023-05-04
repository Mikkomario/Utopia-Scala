package utopia.firmament.image

import utopia.firmament.context.ColorContext
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.collection.template.MapAccess
import utopia.flow.operator.MaybeEmpty
import utopia.flow.view.immutable.eventful.Fixed
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet, ColorShade, FromShadeFactory}
import utopia.paradigm.shape.shape2d.{Size, Sized}
import utopia.paradigm.transform.SizeAdjustable

import scala.language.implicitConversions

object SingleColorIcon
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An empty icon
	  */
	val empty = new SingleColorIcon(Image.empty)
	/**
	 * A pointer that always contains an empty icon
	 */
	val alwaysEmpty = Fixed(empty)
	
	
	// IMPLICIT ---------------------------
	
	implicit def iconToImage(icon: SingleColorIcon)(implicit context: ColorContext): Image = icon.contextual
}

/**
  * Used for displaying single color icons in various contexts
  * @author Mikko Hilpinen
  * @since 4.5.2020, Reflection v1.2
  */
class SingleColorIcon(val original: Image)
	extends Sized[SingleColorIcon] with FromShadeFactory[Image] with MaybeEmpty[SingleColorIcon]
		with SizeAdjustable[SingleColorIcon]
{
	// ATTRIBUTES	------------------------
	
	private val paintedImageCache = original.notEmpty match {
		case Some(img) => WeakCache[Color, Image] { c => img.withColorOverlay(c) }
		case None => MapAccess { _: Any => original }
	}
	
	/**
	  * A black version of this icon
	  */
	lazy val black = original.mapIfNotEmpty { _.withAlpha(0.88) }
	/**
	  * A white version of this icon
	  */
	lazy val white = original.mapIfNotEmpty { _.withColorOverlay(Color.white) }
	/**
	  * A version of this icon for light image + text buttons
	  */
	lazy val blackForButtons = ButtonImageSet.lowAlphaOnDisabled(black)
	/**
	  * A version of this icon for dark image + text buttons
	  */
	lazy val whiteForButtons = ButtonImageSet.lowAlphaOnDisabled(white)
	/**
	  * A version of this icon for black individual buttons
	  */
	lazy val blackIndividualButton = ButtonImageSet.brightening(black)
	/**
	  * A version of this icon for white individual buttons
	  */
	lazy val whiteIndividualButton = ButtonImageSet.darkening(white)
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @return Access to methods that generate button image sets suitable for icon buttons
	  */
	def asButton = IconAsButton
	/**
	  * @return Access to methods that generate images suitable to be used inside buttons
	  */
	def inButton = IconInsideButton
	
	/**
	  * @return A full size version of this icon where icon size matches the source resolution
	  */
	def fullSize = {
		val newIcon = original.withOriginalSize
		if (original == newIcon)
			this
		else
			new SingleColorIcon(newIcon)
	}
	
	/**
	  * @param context Implicit component creation context
	  * @return A black or a white icon, whichever is better suited to the current context
	  */
	def contextual(implicit context: ColorContext) = against(context.background)
	
	
	// IMPLEMENTED  -----------------------
	
	override def self = this
	
	override def isEmpty: Boolean = original.isEmpty
	
	override def size = original.size
	
	override def withSize(size: Size) = map { _.withSize(size) }
	
	/**
	  * @param shade Targeted shade
	  * @return This icon converted to a black or white image matching that shade
	  */
	override def apply(shade: ColorShade) = shade match {
		case Light => white
		case Dark => black
	}
	
	override def *(mod: Double): SingleColorIcon = map { _ * mod }
	
	
	// OTHER	---------------------------
	
	/**
	  * @param color A color
	  * @return This icon painted with that color
	  */
	def apply(color: Color) = paintedImageCache(color)
	/**
	  * @param role Icon color role
	  * @param preferredShade Preferred color shade (default = standard)
	  * @param context Implicit color context
	  * @return This icon converted to an image with a color
	  */
	def apply(role: ColorRole, preferredShade: ColorLevel = Standard)(implicit context: ColorContext): Image =
		apply(context.color.preferring(preferredShade)(role))
	
	/**
	  * @param f A mapping function
	  * @return A mapped copy of this icon
	  */
	def map(f: Image => Image) = new SingleColorIcon(f(original))
	
	/**
	  * @param color Target icon color
	  * @return A button image set to be used in buttons without text
	  */
	@deprecated("Replaced with .forButton.apply(Color)")
	def asIndividualButtonWithColor(color: Color) = {
		val colored = asImageWithColor(color)
		if (color.relativeLuminance < 0.6)
			ButtonImageSet.brightening(colored)
		else
			ButtonImageSet.darkening(colored)
	}
	/**
	  * @param shade a color shade
	  * @return A version of this icon that matches that shade
	  */
	@deprecated("Replaced with .apply(ColorShade)", "v1.0")
	def withShade(shade: ColorShade) = shade match {
		case ColorShade.Light => white
		case ColorShade.Dark => black
	}
	/**
	  * @param color Button background color
	  * @return Images used with specified button background
	  */
	@deprecated("Replaced with .forButton.against(Color)", "v1.0")
	def forButtonWithBackground(color: Color) = inButton.against(color)
	/**
	  * @param iconColor New color of the icon
	  * @return An icon image with specified color overlay
	  */
	@deprecated("Replaced with .apply(Color)", "v1.0")
	def asImageWithColor(iconColor: Color) = paintedImageCache(iconColor)
	/**
	  * @param background Background color
	  * @return A version of this icon (black or white) that is better against that background
	  */
	@deprecated("Replaced with .against(Color)", "v1.0")
	def singleColorImageAgainst(background: Color) = background.shade match {
		case Dark => white
		case Light => black
	}
	
	
	// NESTED   --------------------------
	
	object IconAsButton extends FromShadeFactory[ButtonImageSet]
	{
		// COMPUTED --------------------------
		
		/**
		  * @param context Implicit button creation context
		  * @return A set of black or white images that may be used in a button
		  */
		def contextual(implicit context: ColorContext) = against(context.background)
		
		
		// IMPLEMENTED  ----------------------
		
		/**
		  * @param shade Targeted color shade
		  * @return A button image set with black or white icons matching that shade
		  */
		override def apply(shade: ColorShade) = shade match {
			case Light => whiteIndividualButton
			case Dark => blackIndividualButton
		}
		
		/**
		  * @param colors Color set to use
		  * @param context Implicit component creation context
		  * @return A coloured button image set to use
		  */
		def apply(colors: ColorSet)(implicit context: ColorContext): ButtonImageSet =
			apply(colors.against(context.background))
		/**
		  * @param color Color role to use
		  * @param context Implicit component creation context
		  * @return A colored button image set to use
		  */
		def apply(color: ColorRole)(implicit context: ColorContext): ButtonImageSet =
			apply(context.color(color))
		
		
		// OTHER    -------------------------
		
		/**
		  * @param color Target icon color
		  * @return A button image set that uses icons with that color
		  */
		def apply(color: Color) = {
			val colored = SingleColorIcon.this.apply(color)
			if (color.relativeLuminance < 0.6)
				ButtonImageSet.brightening(colored)
			else
				ButtonImageSet.darkening(colored)
		}
	}
	
	object IconInsideButton extends FromShadeFactory[ButtonImageSet]
	{
		// COMPUTED -------------------------
		
		/**
		  * @param context Component creation context
		  * @return A button image set based on this icon, where only alpha values may change
		  */
		def contextual(implicit context: ColorContext) = against(context.background)
		
		
		// IMPLEMENTED  ---------------------
		
		override def apply(shade: ColorShade): ButtonImageSet = shade match {
			case Dark => blackForButtons
			case Light => whiteForButtons
		}
	}
}
