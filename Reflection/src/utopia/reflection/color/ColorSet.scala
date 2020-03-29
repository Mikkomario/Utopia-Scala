package utopia.reflection.color

import utopia.genesis.color.Color

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
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring the default color)
	  * @param backgroundColor A background / contrasting color
	  * @return The best color in this color set in a context with specified color
	  */
	def forBackground(backgroundColor: Color) =
	{
		val contrastLuminosity = backgroundColor.luminosity
		if ((default.luminosity - contrastLuminosity).abs > 0.25)
			default
		else if ((light.luminosity - contrastLuminosity).abs > (dark.luminosity - contrastLuminosity).abs)
			light
		else
			dark
	}
}
