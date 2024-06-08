package utopia.paradigm.color

import utopia.flow.collection.immutable.Single
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum

import scala.annotation.tailrec
import scala.language.implicitConversions

object ColorSet
{
	// IMPLICIT	--------------------------
	
	/**
	  * @param set A color set
	  * @return Default color for the set
	  */
	implicit def setToColor(set: ColorSet): Color = set.default
	
	
	// ATTRIBUTES   -----------------------
	
	/**
	  * Default shade of gray to use in light themed uis
	  */
	val defaultLightGray = apply(
		Rgb.grayWithValue(225), Rgb.grayWithValue(245), Rgb.grayWithValue(200))
	/**
	  * Default shade of gray to use in dark themed uis
	  */
	val defaultDarkGray = apply(
		Rgb.grayWithValue(66), Rgb.grayWithValue(109), Rgb.grayWithValue(27))
	
	
	// COMPUTED	---------------------------
	
	private def defaultMinimumContrast = Minimum.largeTextMinimumContrast
	
	
	// OTHER	---------------------------
	
	/**
	 * @param default  The default color shade
	 * @param variance Amount of "impact" applied when switching shade to lighter or darker.
	 *                 Default = 2.0 = 2 standard highlight levels
	 * @return A color set based on the specified color with standard variance
	 */
	def apply(default: Color, variance: Double = 2.0): ColorSet =
		apply(default, default.lightenedBy(variance), default.darkenedBy(variance))
	/**
	  * @param default Default color
	  * @param light Lighter version of the default color
	  * @param dark Darker version of the default color
	  * @return A color set
	  */
	def apply(default: Color, light: Color, dark: Color): ColorSet =
		apply(default, Map[ColorShade, Color](Light -> light, Dark -> dark))
	
	/**
	  * @param color A color
	  * @return A color set that only uses that single color
	  */
	def invariant(color: Color) = apply(color, Map[ColorShade, Color]())
	
	/**
	  * Converts a set of hexes into a color set
	  * @param defaultHex A hex for the standard color
	  * @param lightHex   A hex for the light version
	  * @param darkHex    A hex for the dark version
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
  * @since 17.11.2019, Reflection v1
  * @param default The default color
  * @param variants Variants of the default color
  */
case class ColorSet(default: Color, variants: Map[ColorShade, Color])
{
	import ColorSet.defaultMinimumContrast
	
	// COMPUTED	----------------------------
	
	/**
	  * @return A lighter variant of this color
	  */
	def light = variants.getOrElse(Light, default)
	/**
	  * @return A darker variant of this color
	  */
	def dark = variants.getOrElse(Dark, default)
	
	/**
	  * @return Color options in this setin no specific order
	  */
	def values = Single(default) ++ variants.valuesIterator
	
	/*
	  * @param context Color context
	  * @return A color from this set most suited for that context (preferring default shade)
	  */
	// def inContext(implicit context: ColorContextLike) = forBackground(context.containerBackground)
	
	/*
	  * @param context Color context
	  * @return A color from this set most suited for that context (preferring light shade)
	  */
	/*
	def lightInContext(implicit context: ColorContextLike) =
		forBackgroundPreferringLight(context.containerBackground)
	 */
	/*
	  * @param context Color context
	  * @return A color from this set most suited for that context (preferring dark shade)
	  */
	/*
	def darkInContext(implicit context: ColorContextLike) =
		forBackgroundPreferringDark(context.containerBackground)
	*/
	
	
	// OTHER	----------------------------
	
	/**
	  * @param shade Target color shade
	  * @return A color from this set that matches that shade
	  */
	def apply(shade: ColorLevel) = shade match {
		case Standard => default
		case variant: ColorShade => variants.getOrElse(variant, default)
	}
	
	/*
	  * @param shade   Preferred color shade
	  * @param context Component color context
	  * @return A color from this set most suited for that context, preferring the specified shade
	  */
	// def inContextPreferring(shade: ColorShade)(implicit context: ColorContextLike) =
	//	forBackgroundPreferring(context.containerBackground, shade)
	
	/**
	  * Picks the color in this set that is suitable for the specified background color
	  * @param backgroundColor A background / contrasting color
	  * @param preference      Preferred color shade
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return A color in this set most suitable against the specified background color
	  */
	def against(backgroundColor: Color, preference: ColorLevel = Standard,
	            minimumContrast: Double = defaultMinimumContrast): Color =
		preference match {
			// Case: Prefers the default color variant
			case Standard =>
				// Case: Default is OK => Uses it
				if (default.contrastAgainst(backgroundColor) > minimumContrast)
					default
				// Case: Default contrast is too low => Uses the better variant
				else
					variants.valuesIterator.maxByOption { _.contrastAgainst(backgroundColor) }.getOrElse(default)
			// Case: Prefers a color variant => Uses the value with enough contrast
			case variant: ColorShade => against(backgroundColor, variant, minimumContrast)
		}
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring light color)
	  * @param backgroundColor A background / contrasting color
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return The best color in this color set in a context with specified color
	  */
	def againstPreferringLight(backgroundColor: Color,
	                                 minimumContrast: Double = defaultMinimumContrast): Color =
		against(backgroundColor, Light, minimumContrast)
	
	/**
	  * Picks the best color set for the specific background (best being one that has enough contrast difference,
	  * preferring dark color)
	  * @param backgroundColor A background / contrasting color
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return The best color in this color set in a context with specified color
	  */
	def againstPreferringDark(backgroundColor: Color,
	                                minimumContrast: Double = defaultMinimumContrast): Color =
		against(backgroundColor, Dark, minimumContrast)
	
	/**
	  * Picks the color that most resembles the specified color
	  * @param anotherColor Another color
	  * @return A color in this set that most resembles the specified color
	  */
	def mostLike(anotherColor: Color) =
		(Some(default) ++ variants.valuesIterator).minBy { _.contrastAgainst(anotherColor) }
	
	/**
	  * Picks a shade from this color set that works best against multiple colors
	  * @param colors          A set of colors selected color should work with
	  * @param minimumContrast Minimum contrast level required for simply picking the default color
	  *                        (default = minimum legibility default = 4.5:1)
	  * @return The best color in this set to be used against those colors
	  */
	@tailrec
	final def againstMany(colors: Iterable[Color], preference: ColorLevel = Standard,
	                minimumContrast: Double = defaultMinimumContrast): Color =
	{
		val preferred = apply(preference)
		// Case: There is enough contrast against the preferred option => Uses that one
		if (colors.forall { preferred.contrastAgainst(_) >= minimumContrast })
			preferred
		// Case: Not enough contrast for the preferred option => Uses alternative options
		else
			preference match {
				// Case: Standard option was not OK => Uses the better variant
				case Standard =>
					variants.valuesIterator
						.maxByOption { c => colors.map { c.contrastAgainst(_) }.reduce { _ + _ } }.getOrElse(default)
				// Case: Preferred variant was not OK => Uses the standard shade or the better variant (recursive)
				case _: ColorShade => againstMany(colors, minimumContrast = minimumContrast)
			}
	}
	
	/**
	  * @param color A color
	  * @return Whether this color set specifies that color
	  */
	def contains(color: Color) = default == color || variants.valuesIterator.contains(color)
	
	/**
	  * @param f A color mapping function
	  * @return A copy of this color set with mapped colors
	  */
	def map(f: Color => Color) = ColorSet(f(default), variants.view.mapValues(f).toMap)
	
	private def against(backgroundColor: Color, preference: ColorShade, minimumContrast: Double) =
	{
		val order = (variants.get(preference).toVector :+ default) ++ variants.get(preference.opposite)
		val contrasts = order.map { color => color -> Lazy { color.contrastAgainst(backgroundColor) } }
		// Finds the first shade that has enough contrast to the background
		// If none of the shades are suitable, picks one with the greatest contrast
		contrasts.find { _._2.value >= minimumContrast }.getOrElse { contrasts.maxBy { _._2.value } }._1
	}
}
