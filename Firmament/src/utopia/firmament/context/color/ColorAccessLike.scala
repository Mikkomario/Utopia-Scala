package utopia.firmament.context.color

import utopia.paradigm.color.ColorRole._
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for access point to contextual colors
  * @tparam C Type of accessed colors
  * @tparam Repr Type of the implementing class
  * @author Mikko Hilpinen
  * @since 27.09.2024, v1.3.2
  */
trait ColorAccessLike[+C, +Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Access to colors when small objects need to be recognized
	  */
	def expectingSmallObjects: Repr
	/**
	  * @return Access to colors when only large objects need to be recognized
	  */
	def expectingLargeObjects: Repr
	/**
	  * @return Access to colors where contrast is suitable for the current font settings
	  */
	def forText: Repr
	
	/**
	  * @param level A preferred color level
	  * @return Access that prefers that color level
	  */
	def preferring(level: ColorLevel): Repr
	
	/**
	  * @param color A proposed set of colors
	  * @return The best color from the specified set for this context
	  */
	def apply(color: ColorSet): C
	/**
	  * @param role A color role
	  * @return Color to use for that role in this context
	  */
	def apply(role: ColorRole): C
	/**
	  * @param role A color role
	  * @param competingColor A color the resulting color should not resemble
	  * @param moreColors More excluded colors
	  * @return A color of the specified role that is as different as possible
	  *         from the specified colors and the current background color
	  */
	def differentFrom(role: ColorRole, competingColor: Color, moreColors: Color*): C
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Access to light colors
	  */
	def light = preferring(Light)
	/**
	  * @return Access to dark colors
	  */
	def dark = preferring(Dark)
	
	/**
	  * @return Gray color to use
	  */
	def gray = apply(Gray)
	
	/**
	  * @return Primary color to use
	  */
	def primary = apply(Primary)
	/**
	  * @return Secondary color to use
	  */
	def secondary = apply(Secondary)
	/**
	  * @return Tertiary color to use
	  */
	def tertiary = apply(Tertiary)
	
	/**
	  * @return Color to use to represent success
	  */
	def success = apply(Success)
	/**
	  * @return Color to use to represent an error situation or a failure
	  */
	def failure = apply(Failure)
	/**
	  * @return Color to use to represent a warning or danger
	  */
	def warning = apply(Warning)
	/**
	  * @return Color to use to represent additional information or notifications
	  */
	def info = apply(Info)
}
