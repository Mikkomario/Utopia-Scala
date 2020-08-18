package utopia.reflection.color

import utopia.genesis.color.Color
import utopia.reflection.color.ColorShade.{Dark, Light, Standard}

import scala.math.Ordering.Double.TotalOrdering
import scala.language.implicitConversions

object ColorSet
{
	/**
	  * @param set A color set
	  * @return Default color for the set
	  */
	implicit def setToColor(set: ColorSet): Color = set.default
	
	/**
	  * Converts a set of hexes into a color set
	  * @param defaultHex A hex for the standard color
	  * @param lightHex A hex for the light version
	  * @param darkHex A hex for the dark version
	  * @return A color set based on the hexes. Fails if some values couldn't be parsed
	  */
	def fromHexes(defaultHex: String, lightHex: String, darkHex: String) = Color.fromHex(defaultHex).flatMap { normal =>
		Color.fromHex(lightHex).flatMap { light => Color.fromHex(darkHex).map { dark => ColorSet(normal, light, dark) } }
	}
}

/**
  * A set of related colors
  * @author Mikko Hilpinen
  * @since 17.11.2019, v1
  * @param default The default color
  * @param light A lighter version of color
  * @param dark a darker version of color
  */
case class ColorSet(default: ComponentColor, light: ComponentColor, dark: ComponentColor)
{
	/**
	  * @param shade Target color shade
	  * @return A color from this set that matches that shade
	  */
	def apply(shade: ColorShade) = shade match
	{
		case Standard => default
		case Light => light
		case Dark => dark
	}
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring the default color)
	  * @param backgroundColor A background / contrasting color
	  * @return The best color in this color set in a context with specified color
	  */
	def forBackground(backgroundColor: Color): ComponentColor =
	{
		val contrastLuminosity = backgroundColor.luminosity
		if ((default.luminosity - contrastLuminosity).abs > 0.2)
			default
		else
			Vector(light, dark).maxBy { c => (c.luminosity - contrastLuminosity).abs }
	}
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring light color)
	  * @param backgroundColor A background / contrasting color
	  * @return The best color in this color set in a context with specified color
	  */
	def forBackgroundPreferringLight(backgroundColor: Color): ComponentColor = forBackground(backgroundColor, Light)
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring dark color)
	  * @param backgroundColor A background / contrasting color
	  * @return The best color in this color set in a context with specified color
	  */
	def forBackgroundPreferringDark(backgroundColor: Color): ComponentColor = forBackground(backgroundColor, Dark)
	
	/**
	  * Picks the color in this set that is suitable for the specified background color, preferring specified shade
	  * @param backgroundColor A background / contrasting color
	  * @param shade Preferred color shade
	  * @return A color in this set most suitable against the specified background color
	  */
	def forBackgroundPreferring(backgroundColor: Color, shade: ColorShade): ComponentColor = shade match
	{
		case Standard => forBackground(backgroundColor)
		case variant: ColorShadeVariant => forBackgroundPreferring(backgroundColor, variant)
	}
	
	/**
	  * Picks the color that most resembles the specified color
	  * @param anotherColor Another color
	  * @return A color in this set that most resembles the specified color
	  */
	def mostLike(anotherColor: Color) =
	{
		val contrastLuminosity = anotherColor.luminosity
		Vector(default, light, dark).minBy { c => (c.luminosity - contrastLuminosity).abs }
	}
	
	/**
	  * @param color A color
	  * @return Whether this color set specifies that color
	  */
	def contains(color: ComponentColor) = color == default || color == light || color == dark
	
	private def forBackground(backgroundColor: Color, preference: ColorShadeVariant) =
	{
		val order = Vector(preference, Standard, preference.opposite).map(apply)
		val contrastLuminosity = backgroundColor.luminosity
		// Finds the first shade that has enough contrast to the background
		// If none of the shades are suitable, picks one with the greatest contrast
		order.find { shade => (shade.luminosity - contrastLuminosity).abs > 0.2 }
			.getOrElse { order.maxBy { shade => (shade.luminosity - contrastLuminosity).abs } }
	}
}
