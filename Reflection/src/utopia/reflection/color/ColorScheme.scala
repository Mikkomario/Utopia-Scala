package utopia.reflection.color

import utopia.genesis.color.RGB
import utopia.reflection.color.ColorRole.{Error, Gray, Info, Primary, Secondary, Success, Tertiary, Warning}
import utopia.reflection.color.ColorScheme.{defaultError, defaultSuccess, defaultWarning}

object ColorScheme
{
	// ATTRIBUTES	-----------------------------
	
	/**
	  * Default shade of gray to use in light themed uis
	  */
	val defaultLightGray = ColorSet(RGB.grayWithValue(225), RGB.grayWithValue(245), RGB.grayWithValue(225))
	/**
	  * Default shade of gray to use in dark themed uis
	  */
	val defaultDarkGray = ColorSet(RGB.grayWithValue(66), RGB.grayWithValue(109), RGB.grayWithValue(27))
	/**
	  * Default error color to use
	  */
	val defaultError = ColorSet(RGB.withValues(176, 0, 32), RGB.withValues(233, 73, 72), RGB.withValues(121, 0, 0))
	/**
	  * Default warning color to use
	  */
	val defaultWarning = ColorSet(RGB.withValues(239, 159, 0), RGB.withValues(255, 208, 72), RGB.withValues(183, 113, 0))
	/**
	  * Default success color to use
	  */
	val defaultSuccess = ColorSet(RGB.withValues(44, 181, 17), RGB.withValues(107, 232, 77), RGB.withValues(0, 132, 0))
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a color scheme that mainly uses a single color
	  * @param color Primary color used
	  * @param gray Grayscale color used (default = light gray)
	  * @return A new color scheme
	  */
	def monochrome(color: ColorSet, gray: ColorSet = defaultLightGray) = apply(color, gray)
	
	/**
	  * Creates a color scheme that uses two main colors
	  * @param primary Primary color used
	  * @param secondary Secondary color used
	  * @param gray Grayscale color used (default = light gray)
	  * @return A new color scheme
	  */
	def twoTone(primary: ColorSet, secondary: ColorSet, gray: ColorSet = defaultLightGray) = apply(primary, gray,
		Map(Secondary -> secondary, Error -> defaultError, Warning -> defaultWarning, Success -> defaultSuccess))
	
	/**
	  * Creates a color scheme that uses three main colors
	  * @param primary Primary color used
	  * @param secondary Secondary color used
	  * @param tertiary Tertiary color used
	  * @param gray Grayscale color used (default = light gray)
	  * @return A new color scheme
	  */
	def threeTone(primary: ColorSet, secondary: ColorSet, tertiary: ColorSet, gray: ColorSet = defaultLightGray) =
		apply(primary, gray, Map(Secondary -> secondary, Tertiary -> tertiary, Error -> defaultError,
			Warning -> defaultWarning, Success -> defaultSuccess))
}

/**
  * Defines program default colors
  * @author Mikko Hilpinen
  * @since 17.11.2019, v1
  * @param primary The primary color's used
  * @param gray supplementary grayscale colors used (default = light gray)
  * @param additional Additional colors used [Color role -> Color set]
  *                   (default = only error, warning and success colors specified)
  */
case class ColorScheme(primary: ColorSet, gray: ColorSet = ColorScheme.defaultLightGray,
					   additional: Map[AdditionalColorRole, ColorSet] =
					   Map(Error -> defaultError, Success -> defaultSuccess, Warning -> defaultWarning))
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Standard ui colors (primary, secondary, tertiary) available in this color scheme (1-3 elements)
	  */
	lazy val standardColors = Vector(primary) ++ additional.get(Secondary) ++ additional.get(Tertiary)
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return Color to use as secondary standard color
	  */
	def secondary = apply(Secondary)
	
	/**
	  * @return Color to use as tertiary standard color
	  */
	def tertiary = apply(Tertiary)
	
	/**
	  * @return Color to use in error situations
	  */
	def error = apply(Error)
	
	/**
	  * @return Color to use when displaying warnings
	  */
	def warning = apply(Warning)
	
	/**
	  * @return Color used when displaying success indications
	  */
	def success = apply(Success)
	
	/**
	  * @return Color used when displaying additional information
	  */
	def info = apply(Info)
	
	/**
	  * @return Number of different standard colors (primary, secondary, tertiary) defined in this color scheme (1-3)
	  */
	def numberOfStandardColors = standardColors.size
	
	
	// OTHER	------------------------------
	
	/**
	  * @param role A color role
	  * @return A color in this set that should be used for that role
	  */
	def apply(role: ColorRole): ColorSet = role match
	{
		case Primary => primary
		case Gray => gray
		case variant: AdditionalColorRole => additional.getOrElse(variant, apply(variant.backup))
	}
	
	/**
	  * @param color An additional color specification
	  * @return A copy of this scheme with specified color included
	  */
	def +(color: (ColorRole, ColorSet)) = color._1 match
	{
		case Primary => copy(primary = color._2)
		case Gray => copy(gray = color._2)
		case variant: AdditionalColorRole =>
			val newMap = additional + (variant -> color._2)
			copy(additional = newMap)
	}
}
