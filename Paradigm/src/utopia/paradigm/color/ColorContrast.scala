package utopia.paradigm.color

import utopia.flow.operator.numeric.DoubleLike
import utopia.flow.operator.sign.{Sign, SignOrZero}
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.paradigm.enumeration.ColorContrastStandard.Enhanced
import utopia.paradigm.measurement.Distance

import scala.language.implicitConversions

object ColorContrast
{
	// IMPLICIT	---------------------------
	
	// Automatically converts a color contrast to a double value
	implicit def contrastToDouble(contrast: ColorContrast): Double = contrast.ratio
}

/**
  * Represents a contrast between two colors
  * @author Mikko Hilpinen
  * @since Genesis 28.1.2021, v2
  * @param ratio Contrast ratio between the two colors (E.g. 7:1 (= 7.0))
  */
case class ColorContrast(ratio: Double) extends DoubleLike[ColorContrast]
{
	// ATTRIBUTES	----------------------
	
	/**
	  * The color contrast standard reached in a normal context
	  */
	lazy val standard = ColorContrastStandard.forContrast(ratio)
	/**
	  * The color contrast standard reached in context where large text is used
	  */
	lazy val largeTextStandard = ColorContrastStandard.values.find { ratio >= _.largeTextMin }
	
	
	// COMPUTED	--------------------------
	
	/**
	 * @return Whether this color contrast ensures enhanced legibility on all font sizes
	 */
	def isAlwaysHighQuality = isAlwaysLegible(Enhanced)
	/**
	 * @return Whether this color contrast ensures enhanced legibility on large font sizes
	 */
	def isHighQualityForLargeText = isLegibleForLargeText(Enhanced)
	
	/**
	 * @param context Applicable color contrast requirements (implicit)
	  * @return Whether this color contrast ensures minimum legibility on all font sizes
	  */
	def isAlwaysLegible(implicit context: HasColorContrastRequirements) = context.requiredContrast.accepts(ratio)
	/**
	 * @param context Applicable color contrast requirements (implicit)
	  * @return Whether this color contrast ensures minimum legibility for large text sizes
	  */
	def isLegibleForLargeText(implicit context: HasColorContrastRequirements) =
		context.requiredContrast.accepts(ratio, large = true)
	
	
	// IMPLEMENTED	----------------------
	
	override def self = this
	
	override def sign: SignOrZero = Sign.of(ratio)
	override def length = ratio
	
	override def zero = ColorContrast(0)
	
	def -(another: ColorContrast) = ColorContrast(ratio - another.ratio)
	override def +(another: ColorContrast) = ColorContrast(ratio + another.ratio)
	override def *(mod: Double) = ColorContrast(ratio * mod)
	
	override def compareTo(o: ColorContrast) = ratio.compareTo(o.ratio)
	
	
	// OTHER	--------------------------
	
	/**
	  * @param amount Contrast amount to increase
	  * @return An increased contrast
	  */
	def +(amount: Double) = ColorContrast(ratio + amount)
	/**
	  * @param amount Contrast amount to decrease
	  * @return A decreased contrast
	  */
	def -(amount: Double) = ColorContrast(ratio - amount)
	
	/**
	  * @param fontSize Size of the font used
	  * @param bold Whether the font is bold (default = false)
	  * @return Whether text would be legible with this color contrast and specified settings
	  */
	def isLegibleWithFontSize(fontSize: Distance, bold: Boolean = false)
	                         (implicit context: HasColorContrastRequirements) =
		context.requiredContrast.acceptsText(ratio, fontSize, bold = bold)
	/**
	  * @param fontSize Size of the font used
	  * @param bold Whether the font is bold (default = false)
	  * @return Whether text would meet enhanced standards with this color contrast and specified settings
	  */
	def isHighQualityWithFontSize(fontSize: Distance, bold: Boolean = false) =
		isLegibleWithFontSize(fontSize, bold)(Enhanced)
	
	/**
	  * @param fontSize Size of the font used
	  * @param bold Whether the font is bold (default = false)
	  * @return The standard reached with this color contrast and specified settings. None if no standard is reached.
	  */
	def standardWithFontSize(fontSize: Distance, bold: Boolean) =
		ColorContrastStandard.forText(ratio, fontSize, bold)
}
