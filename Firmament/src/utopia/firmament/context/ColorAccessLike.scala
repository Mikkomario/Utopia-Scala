package utopia.firmament.context

import utopia.paradigm.color._

/**
  * Common trait for access point to contextual colors
  * @tparam C Type of accessed colors
  * @tparam Repr Type of the implementing class
  * @author Mikko Hilpinen
  * @since 27.09.2024, v1.4
  */
trait ColorAccessLike[+C, +Repr] extends FromColorRoleFactory[C] with FromShadeFactory[Repr]
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
	  * @param competingColor A color the resulting color should not resemble
	  * @param moreColors More excluded colors
	  * @return A color of the specified role that is as different as possible
	  *         from the specified colors and the current background color
	  */
	def differentFrom(role: ColorRole, competingColor: Color, moreColors: Color*): C
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(shade: ColorShade): Repr = preferring(shade)
}
