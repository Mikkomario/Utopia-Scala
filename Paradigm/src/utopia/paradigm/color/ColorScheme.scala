package utopia.paradigm.color

import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.color.ColorRole._

object ColorScheme
{
	// ATTRIBUTES	-----------------------------
	
	/**
	  * The default color scheme. Has no primary, secondary or tertiary colors specified. Uses lighter gray tone.
	  */
	lazy val default = ColorScheme(ColorSet.defaultDarkGray, Map(
		Gray -> ColorSet.defaultLightGray,
		Failure -> ColorSet(Rgb.withValues(176, 0, 32), Rgb.withValues(233, 73, 72), Rgb.withValues(121, 0, 0)),
		Warning -> ColorSet(Rgb.withValues(239, 159, 0), Rgb.withValues(255, 208, 72), Rgb.withValues(183, 113, 0)),
		Success -> ColorSet(Rgb.withValues(44, 181, 17), Rgb.withValues(107, 232, 77), Rgb.withValues(0, 132, 0))
	))
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a color scheme that uses a single color
	  * @param color Primary color used
	  * @return A new color scheme
	  */
	def monochrome(color: ColorSet) = apply(color, Map(Primary -> color))
	/**
	  * Creates a color scheme that uses two main colors
	  * @param primary   Primary color used
	  * @param secondary Secondary color used
	  * @return A new color scheme
	  */
	def twoTone(primary: ColorSet, secondary: ColorSet) =
		apply(primary, Map(Primary -> primary, Secondary -> secondary))
	/**
	  * Creates a color scheme that uses three main colors
	  * @param primary   Primary color used
	  * @param secondary Secondary color used
	  * @param tertiary  Tertiary color used
	  * @return A new color scheme
	  */
	def threeTone(primary: ColorSet, secondary: ColorSet, tertiary: ColorSet) =
		apply(primary, Map(Primary -> primary, Secondary -> secondary, Tertiary -> tertiary))
}

/**
  * Defines program default colors
  * @author Mikko Hilpinen
  * @since 17.11.2019, Reflection v1
  * @param default Default colors to use when no other color is specified
  * @param colors Defined color values
  */
case class ColorScheme(default: ColorSet, colors: Map[ColorRole, ColorSet])
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Standard ui colors (primary, secondary, tertiary) available in this color scheme (1-3 elements)
	  */
	lazy val standards = Vector(primary) ++ colors.get(Secondary) ++ colors.get(Tertiary)
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return Color roles that have been defined in this color scheme
	  */
	def definedRoles = colors.keySet
	
	/**
	  * @return Gray colors available
	  */
	def gray = apply(Gray)
	
	def primary = apply(Primary)
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
	def failure = apply(Failure)
	/**
	  * @return Color to use when displaying warnings
	  */
	def warning = apply(Warning)
	
	/**
	  * @return Color to use when displaying a success state
	  */
	def success = apply(Success)
	/**
	  * @return Color to use when displaying an info state
	  */
	def info = apply(Info)
	
	
	// OTHER	------------------------------
	
	/**
	  * @param role A color role
	  * @return A color in this set that should be used for that role
	  */
	def apply(role: ColorRole): ColorSet =
		(Iterator.single(role) ++ role.alternativesIterator).findMap(colors.get).getOrElse(default)
	
	/**
	  * @param color An additional color specification
	  * @return A copy of this scheme with specified color included
	  */
	def +(color: (ColorRole, ColorSet)) = copy(default, colors + color)
	/**
	  * @param other Another color scheme
	  * @return Combination of these schemes, where the specified scheme is preferred
	  */
	def ++(other: ColorScheme) = copy(other.default, colors ++ other.colors)
	/**
	  * @param colors A set of colors, tied to the roles in which they serve
	  * @return A copy of this scheme that uses the specified colors
	  */
	def ++(colors: IterableOnce[(ColorRole, ColorSet)]) = copy(colors = this.colors ++ colors)
	
	/**
	  * @param newDefault New default color to use
	  * @return A copy of this color scheme with the specified default value
	  */
	def withDefault(newDefault: ColorSet) = copy(newDefault)
}