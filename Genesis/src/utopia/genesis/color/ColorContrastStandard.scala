package utopia.genesis.color

import utopia.flow.util.RichComparable
import utopia.genesis.color.ColorContrastStandard.{largeTextThreshold, largeTextThresholdBold}
import utopia.genesis.util.Distance

/**
  * Represents different color contrast requirements based on WACG 2
  * @author Mikko Hilpinen
  * @since 28.1.2021, v2.4
  * @see https://webaim.org/articles/contrast/
  */
sealed trait ColorContrastStandard extends RichComparable[ColorContrastStandard]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return The minimum contrast ratio that is normally used
	  */
	def defaultMinimumContrast: Double
	
	/**
	  * @return The minimum contrast ratio that is allowed for large text
	  */
	def largeTextMinimumContrast: Double
	
	
	// OTHER	---------------------------
	
	/**
	  * @param fontSize Font size
	  * @param fontIsBold Whether the font is bold
	  * @return The minimum color contrast requirement in this standard for text with those settings
	  */
	def minimumContrastForText(fontSize: Distance, fontIsBold: Boolean = false) =
	{
		if (fontSize >= (if (fontIsBold) largeTextThresholdBold else largeTextThreshold))
			largeTextMinimumContrast
		else
			defaultMinimumContrast
	}
	
	/**
	  * Tests whether specified text settings meet these color contrast standards
	  * @param contrast Color contrast between the text and the background
	  * @param fontSize Size of the used font
	  * @param fontIsBold Whether the used font is bold (default = false)
	  * @return Whether the text meets this standard
	  */
	def test(contrast: Double, fontSize: Distance, fontIsBold: Boolean = false) =
	{
		// Checks whether text meets higher requirements
		if (contrast >= defaultMinimumContrast)
			true
		else if (contrast < largeTextMinimumContrast)
			false
		else
		{
			// If the result is based on text size (large or normal), measures the text size against the standard
			// The standard is that text with >= 18 points font is considered large, >= 14 points if the text is bold
			// One point is considered to be 1/72 inches in size - inches are used because pixels are a variable
			// and unreliable measurement in this context (a pixel size varies on different devices)
			val minimumFontLength = if (fontIsBold) largeTextThresholdBold else largeTextThreshold
			fontSize >= minimumFontLength
		}
	}
	
	/**
	  * Tests a color contrast against the default contrast threshold in this standard
	  * @param contrast A color contrast
	  * @return Whether that color contrast meets the default threshold in this standard
	  */
	def test(contrast: Double) = contrast >= defaultMinimumContrast
}

object ColorContrastStandard
{
	// ATTRIBUTES	-------------------------
	
	private val largeTextThreshold = Distance.ofInches(18 / 72.0)
	private val largeTextThresholdBold = Distance.ofInches(14 / 72.0)
	
	/**
	  * All registered color contrast standards from more to less strict (enhanced & minimum)
	  */
	lazy val values = Vector[ColorContrastStandard](Enhanced, Minimum)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Finds the highest standard reached by the specified color contrast
	  * @param contrast A color contrast
	  * @return The highest standard reached by that contrast. None if that contrast doesn't meet any standards.
	  */
	def forContrast(contrast: Double) = values.find { _.test(contrast) }
	
	/**
	  * Finds the highest standard reached by a text color configuration
	  * @param contrast Contrast between text color and background
	  * @param fontSize Font size used
	  * @param fontIsBold Whether the font is bold (default = false)
	  * @return The highest standard reached by that text setup. None if that contrast doesn't meet any standards.
	  */
	def forText(contrast: Double, fontSize: Distance, fontIsBold: Boolean = false) =
		values.find { _.test(contrast, fontSize, fontIsBold) }
	
	
	// NESTED	-----------------------------
	
	/**
	  * Minimum allowed WACG 2 contrast standard (1.4.3 AA), which is the minimum required by associated laws
	  */
	case object Minimum extends ColorContrastStandard
	{
		override def defaultMinimumContrast = 4.5
		
		override def largeTextMinimumContrast = 3.0
		
		override def compareTo(o: ColorContrastStandard) = o match
		{
			case Minimum => 0
			case Enhanced => -1
		}
	}
	
	/**
	  * The "enhanced" higher WCAG 2 contrast standard (1.4.6 AAA)
	  */
	case object Enhanced extends ColorContrastStandard
	{
		override def defaultMinimumContrast = 7.0
		
		override def largeTextMinimumContrast = 4.5
		
		override def compareTo(o: ColorContrastStandard) = o match
		{
			case Minimum => 1
			case Enhanced => 0
		}
	}
}