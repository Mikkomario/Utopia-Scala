package utopia.paradigm.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.color.ColorContrastRequirement
import utopia.paradigm.measurement.Distance

/**
  * An enumeration for different color contrast standards, based on WACG 2
  * @author Mikko Hilpinen
  * @since Genesis 28.1.2021, v2.4
  * @see https://webaim.org/articles/contrast/
  */
sealed trait ColorContrastStandard extends ColorContrastRequirement

object ColorContrastStandard
{
	// ATTRIBUTES	-------------------------
	
	/**
	 * All registered color contrast standards from more to less strict (enhanced & minimum)
	 */
	lazy val values = Pair[ColorContrastStandard](Enhanced, Minimum)
	
	
	// OTHER	-----------------------------
	
	/**
	 * Finds the highest standard reached by the specified color contrast
	 * @param contrast A color contrast
	 * @return The highest standard reached by that contrast. None if that contrast doesn't meet any standards.
	 */
	def forContrast(contrast: Double) = values.find { _.accepts(contrast) }
	/**
	 * Finds the highest standard reached by a text color configuration
	 * @param contrast Contrast between text color and background
	 * @param fontSize Font size used
	 * @param fontIsBold Whether the font is bold (default = false)
	 * @return The highest standard reached by that text setup. None if that contrast doesn't meet any standards.
	 */
	def forText(contrast: Double, fontSize: Distance, fontIsBold: Boolean = false) =
		values.find { _.acceptsText(contrast, fontSize, fontIsBold) }
	
	
	// VALUES	-----------------------------
	
	/**
	 * Minimum allowed WACG 2 contrast standard (1.4.3 AA), which is the minimum required by associated laws
	 */
	object Minimum extends ColorContrastStandard
	{
		// ATTRIBUTES   --------------------
		
		override val defaultMin: Double = 4.5
		override val largeTextMin: Double = 3.0
		
		
		// IMPLEMENTED  --------------------
		
		override def self = this
	}
	
	/**
	 * The "enhanced" higher WCAG 2 contrast standard (1.4.6 AAA)
	 */
	object Enhanced extends ColorContrastStandard
	{
		// ATTRIBUTES   --------------------
		
		override val defaultMin: Double = 7.0
		override val largeTextMin: Double = 4.5
		
		
		// IMPLEMENTED  --------------------
		
		override def self = this
	}
}