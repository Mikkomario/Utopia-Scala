package utopia.reflection.color

import utopia.flow.datastructure.immutable.Lazy
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum
import utopia.reflection.color.ColorShade.{Dark, Light, Standard}
import utopia.reflection.component.context.ColorContextLike

import scala.language.implicitConversions

object ColorSet
{
	// IMPLICIT	--------------------------
	
	/**
	  * @param set A color set
	  * @return Default color for the set
	  */
	implicit def setToColor(set: ColorSet): Color = set.default
	
	
	// COMPUTED	---------------------------
	
	private def defaultMinimumContrast = Minimum.largeTextMinimumContrast
	
	
	// OTHER	---------------------------
	
	/**
	  * Converts a set of hexes into a color set
	  * @param defaultHex A hex for the standard color
	  * @param lightHex A hex for the light version
	  * @param darkHex A hex for the dark version
	  * @return A color set based on the hexes. Fails if some values couldn't be parsed
	  */
	def fromHexes(defaultHex: String, lightHex: String, darkHex: String) =
		Color.fromHex(defaultHex).flatMap { normal =>
			Color.fromHex(lightHex).flatMap { light =>
				Color.fromHex(darkHex).map { dark => ColorSet(normal, light, dark) }
			}
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
	import ColorSet.defaultMinimumContrast
	
	// COMPUTED	----------------------------
	
	/**
	  * @return Color options in this set from light to dark
	  */
	def values = Vector(light, default, dark)
	
	/**
	  * @param context Color context
	  * @return A color from this set most suited for that context (preferring default shade)
	  */
	def inContext(implicit context: ColorContextLike) = forBackground(context.containerBackground)
	
	/**
	  * @param context Color context
	  * @return A color from this set most suited for that context (preferring light shade)
	  */
	def lightInContext(implicit context: ColorContextLike) =
		forBackgroundPreferringLight(context.containerBackground)
	
	/**
	  * @param context Color context
	  * @return A color from this set most suited for that context (preferring dark shade)
	  */
	def darkInContext(implicit context: ColorContextLike) =
		forBackgroundPreferringDark(context.containerBackground)
	
	
	// OTHER	----------------------------
	
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
	  * @param shade Preferred color shade
	  * @param context Component color context
	  * @return A color from this set most suited for that context, preferring the specified shade
	  */
	def inContextPreferring(shade: ColorShade)(implicit context: ColorContextLike) =
		forBackgroundPreferring(context.containerBackground, shade)
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring the default color)
	  * @param backgroundColor A background / contrasting color
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return The best color in this color set in a context with specified color
	  */
	def forBackground(backgroundColor: Color, minimumContrast: Double = defaultMinimumContrast): ComponentColor =
	{
		if (default.contrastAgainst(backgroundColor) > minimumContrast)
			default
		else
			Vector(light, dark).maxBy { _.contrastAgainst(backgroundColor) }
	}
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring light color)
	  * @param backgroundColor A background / contrasting color
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return The best color in this color set in a context with specified color
	  */
	def forBackgroundPreferringLight(backgroundColor: Color,
									 minimumContrast: Double = defaultMinimumContrast): ComponentColor =
		forBackground(backgroundColor, Light, minimumContrast)
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring dark color)
	  * @param backgroundColor A background / contrasting color
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return The best color in this color set in a context with specified color
	  */
	def forBackgroundPreferringDark(backgroundColor: Color,
									minimumContrast: Double = defaultMinimumContrast): ComponentColor =
		forBackground(backgroundColor, Dark, minimumContrast)
	
	/**
	  * Picks the color in this set that is suitable for the specified background color, preferring specified shade
	  * @param backgroundColor A background / contrasting color
	  * @param shade Preferred color shade
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return A color in this set most suitable against the specified background color
	  */
	def forBackgroundPreferring(backgroundColor: Color, shade: ColorShade,
								minimumContrast: Double = defaultMinimumContrast): ComponentColor =
		shade match
		{
			case Standard => forBackground(backgroundColor, minimumContrast)
			case variant: ColorShadeVariant => forBackground(backgroundColor, variant, minimumContrast)
		}
	
	/**
	  * Picks the color that most resembles the specified color
	  * @param anotherColor Another color
	  * @return A color in this set that most resembles the specified color
	  */
	def mostLike(anotherColor: Color) =
	{
		Vector(default, light, dark).minBy { _.contrastAgainst(anotherColor) }
	}
	
	/**
	  * Picks a shade from this color set that works best against multiple colors
	  * @param colors A set of colors selected color should work with
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return The best color in this set to be used against those colors
	  */
	def bestAgainst(colors: Iterable[Color], minimumContrast: Double = defaultMinimumContrast) =
	{
		if (colors.forall { default.contrastAgainst(_) >= minimumContrast })
			default
		else
			Vector(default, light, dark).maxBy { c => colors.map { c.contrastAgainst(_) }.reduce { _ + _ } }
	}
	
	/**
	  * @param color A color
	  * @return Whether this color set specifies that color
	  */
	def contains(color: ComponentColor) = color == default || color == light || color == dark
	
	/**
	 * @param f A color mapping function
	 * @return A copy of this color set with mapped colors
	 */
	def map(f: ComponentColor => ComponentColor) = ColorSet(f(default), f(light), f(dark))
	
	private def forBackground(backgroundColor: Color, preference: ColorShadeVariant, minimumContrast: Double) =
	{
		val order = Vector(preference, Standard, preference.opposite).map(apply)
		val contrasts = order.map { color => color -> Lazy { color.contrastAgainst(backgroundColor) } }
		// Finds the first shade that has enough contrast to the background
		// If none of the shades are suitable, picks one with the greatest contrast
		contrasts.find { _._2.value >= minimumContrast }.getOrElse { contrasts.maxBy { _._2.value } }._1
	}
}
