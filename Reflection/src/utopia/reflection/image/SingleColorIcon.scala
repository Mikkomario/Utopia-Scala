package utopia.reflection.image

import utopia.flow.caching.multi.WeakCache
import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.reflection.color.{ColorSet, ColorShade, ColorShadeVariant, ComponentColor}
import utopia.reflection.color.TextColorStandard.{Dark, Light}
import utopia.reflection.component.context.{ButtonContextLike, ColorContextLike}
import utopia.reflection.component.swing.button.ButtonImageSet

object SingleColorIcon
{
	/**
	  * An empty icon
	  */
	val empty = new SingleColorIcon(Image.empty)
}

/**
  * Used for displaying single color icons in various contexts
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  */
class SingleColorIcon(val original: Image)
{
	// ATTRIBUTES	------------------------
	
	private val paintedImageCache = WeakCache[Color, Image] { c => original.withColorOverlay(c) }
	
	/**
	  * A black version of this icon
	  */
	lazy val black = original.withAlpha(0.88)
	/**
	  * A white version of this icon
	  */
	lazy val white = original.withColorOverlay(Color.white)
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
	  * @return Size of this icon
	  */
	def size = original.size
	
	/**
	  * @return A full size version of this icon where icon size matches the source resolution
	  */
	def fullSize =
	{
		val newIcon = original.withOriginalSize
		if (original == newIcon)
			this
		else
			new SingleColorIcon(newIcon)
	}
	
	/**
	  * @param context Component creation context
	  * @return A copy of this icon as an image with a single color. The color matches contextual text color.
	  */
	def singleColorImage(implicit context: ColorContextLike) =
	{
		val background = context match
		{
			case btnC: ButtonContextLike => btnC.buttonColor
			case c: ColorContextLike => c.containerBackground
		}
		background.textColorStandard match
		{
			case Dark => black
			case Light => white
		}
	}
	
	/**
	  * @param context Button creation context
	  * @return A version of this icon suitable for this button's background
	  */
	def inButton(implicit context: ButtonContextLike) = context.buttonColor.textColorStandard match
	{
		case Dark => blackForButtons
		case Light => whiteForButtons
	}
	
	/**
	  * @param context Context for the button
	  * @return An button image set suitable for that background, based on this icon
	  */
	def asIndividualButton(implicit context: ColorContextLike) = context.containerBackground.textColorStandard match
	{
		case Dark => blackIndividualButton
		case Light => whiteIndividualButton
	}
	
	
	// OTHER	---------------------------
	
	/**
	  * @param color Target icon color
	  * @return A button image set to be used in buttons without text
	  */
	def asIndividualButtonWithColor(color: Color) =
	{
		val colored = asImageWithColor(color)
		if (color.luminosity < 0.6)
			ButtonImageSet.brightening(colored)
		else
			ButtonImageSet.darkening(colored)
	}
	
	/**
	 * @param colors Available colors
	 * @param context Button creation context
	 * @return A button image set to be used in buttons without text
	 */
	def asIndividualButtonWithColor(colors: ColorSet)(implicit context: ColorContextLike): ButtonImageSet =
		asIndividualButtonWithColor(colors.forBackground(context.containerBackground))
	
	/**
	  * @param shade a color shade
	  * @return A version of this icon that matches that shade
	  */
	def withShade(shade: ColorShadeVariant) = shade match
	{
		case ColorShade.Light => white
		case ColorShade.Dark => black
	}
	
	/**
	  * @param f A mapping function
	  * @return A mapped copy of this icon
	  */
	def map(f: Image => Image) = new SingleColorIcon(f(original))
	
	/**
	  * @param color Button background color
	  * @return Images used with specified button background
	  */
	def forButtonWithBackground(color: ComponentColor) = color.textColorStandard match
	{
		case Dark => blackForButtons
		case Light => whiteForButtons
	}
	
	/**
	  * @param iconColor New color of the icon
	  * @return An icon image with specified color overlay
	  */
	def asImageWithColor(iconColor: Color) = paintedImageCache(iconColor)
	
	/**
	  * @param background Background color
	  * @return A version of this icon (black or white) that is better against that background
	  */
	def singleColorImageAgainst(background: ComponentColor) = background.textColorStandard match
	{
		case Dark => black
		case Light => white
	}
}
