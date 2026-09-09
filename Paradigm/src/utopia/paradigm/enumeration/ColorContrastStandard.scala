package utopia.paradigm.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.ordering.SelfComparable
import utopia.paradigm.enumeration.ColorContrastStandard.textIsLarge
import utopia.paradigm.measurement.Distance
import utopia.paradigm.transform.LinearSizeAdjustable

object ColorContrastStandard
{
	// ATTRIBUTES	-------------------------
	
	/**
	 * The threshold for text that is considered large when it is bold
	 */
	val largeTextSizeThreshold = Distance.ofInches(18 / 72.0)
	/**
	 * The threshold for text that is considered large when it is not bold
	 */
	val largeTextSizeThresholdForBold = Distance.ofInches(14 / 72.0)
	
	/**
	 * All registered color contrast standards from more to less strict (enhanced & minimum)
	 */
	@deprecated("Deprecated for removal", "v1.8.2")
	lazy val values = Pair[ColorContrastStandard](Enhanced, Minimum)
	
	
	// COMPUTED -----------------------------
	
	@deprecated("Renamed to largeTextSizeThreshold", "v1.8.2")
	def largeTextThreshold = largeTextSizeThreshold
	@deprecated("Renamed to largeTextSizeThresholdForBold", "v1.8.2")
	def largeTextThresholdBold = largeTextSizeThresholdForBold
	
	
	// OTHER	-----------------------------
	
	/**
	 * Finds the highest standard reached by the specified color contrast
	 * @param contrast A color contrast
	 * @return The highest standard reached by that contrast. None if that contrast doesn't meet any standards.
	 */
	@deprecated("Deprecated for removal", "v1.8.2")
	def forContrast(contrast: Double) = values.find { _.accepts(contrast) }
	/**
	 * Finds the highest standard reached by a text color configuration
	 * @param contrast Contrast between text color and background
	 * @param fontSize Font size used
	 * @param fontIsBold Whether the font is bold (default = false)
	 * @return The highest standard reached by that text setup. None if that contrast doesn't meet any standards.
	 */
	@deprecated("Deprecated for removal", "v1.8.2")
	def forText(contrast: Double, fontSize: Distance, fontIsBold: Boolean = false) =
		values.find { _.acceptsText(contrast, fontSize, fontIsBold) }
	
	/**
	 * @param minContrastDefault Minimum contrast ratio to use by default
	 * @return Color contrast standard applying the specified limits
	 */
	def apply(minContrastDefault: Double): ColorContrastStandard = apply(minContrastDefault, minContrastDefault / 1.5)
	/**
	 * @param minContrastDefault Minimum contrast ratio to use by default
	 * @param largeTextMinContrast Minimum contrast ratio applied to large text & other large surfaces
	 * @return Color contrast standard applying the specified limits
	 */
	def apply(minContrastDefault: Double, largeTextMinContrast: Double): ColorContrastStandard =
		new _ColorContrastStand(minContrastDefault, largeTextMinContrast)
	
	/**
	 * Checks whether text should be considered large.
	 * Different contrast requirements apply for larger text, than for smaller text.
	 * @param fontSize Applicable font size
	 * @param bold Whether bold style is used. Default = false.
	 * @return Whether text with those settings is considered large, in terms of color contrast standards.
	 */
	def textIsLarge(fontSize: Distance, bold: Boolean = false) =
		fontSize >= (if (bold) largeTextSizeThresholdForBold else largeTextSizeThreshold)
	
	
	// NESTED	-----------------------------
	
	/**
	 * Minimum allowed WACG 2 contrast standard (1.4.3 AA), which is the minimum required by associated laws
	 */
	object Minimum extends ColorContrastStandard
	{
		// ATTRIBUTES   --------------------
		
		override val minContrastDefault: Double = 4.5
		override val largeTextMinContrast: Double = 3.0
		
		
		// IMPLEMENTED  --------------------
		
		override def self = this
	}
	
	/**
	 * The "enhanced" higher WCAG 2 contrast standard (1.4.6 AAA)
	 */
	object Enhanced extends ColorContrastStandard
	{
		// ATTRIBUTES   --------------------
		
		override val minContrastDefault: Double = 7.0
		override val largeTextMinContrast: Double = 4.5
		
		
		// IMPLEMENTED  --------------------
		
		override def self = this
	}
	
	private class _ColorContrastStand(override val minContrastDefault: Double,
	                                  override val largeTextMinContrast: Double)
		extends ColorContrastStandard
	{
		override def self: ColorContrastStandard = this
	}
}

/**
  * Represents different color contrast requirements based on WACG 2
  * @author Mikko Hilpinen
  * @since Genesis 28.1.2021, v2.4
  * @see https://webaim.org/articles/contrast/
  */
trait ColorContrastStandard
	extends SelfComparable[ColorContrastStandard] with EqualsBy with LinearSizeAdjustable[ColorContrastStandard]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return The minimum contrast ratio that is normally used
	  */
	def minContrastDefault: Double
	/**
	  * @return The minimum contrast ratio that is allowed for large text
	  */
	def largeTextMinContrast: Double
	
	
	// COMPUTED ---------------------------
	
	@deprecated("Renamed to minContrastDefault", "v1.8.2")
	def defaultMinimumContrast: Double = minContrastDefault
	@deprecated("Renamed to largeTextMinContrast", "v1.8.2")
	def largeTextMinimumContrast: Double = largeTextMinContrast
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def equalsProperties: IterableOnce[Any] = Pair(minContrastDefault, largeTextMinContrast)
	
	override def compareTo(o: ColorContrastStandard): Int = {
		if (minContrastDefault == o.minContrastDefault)
			largeTextMinContrast.compareTo(o.largeTextMinContrast)
		else
			minContrastDefault.compareTo(o.minContrastDefault)
	}
	
	override def *(mod: Double): ColorContrastStandard =
		ColorContrastStandard(minContrastDefault * mod, largeTextMinContrast * mod)
	
	
	// OTHER	---------------------------
	
	/**
	  * @param large Whether the viewed object or text is large (true) or small (false)
	  * @return Minimum contrast for objects of that size
	  */
	def minContrastFor(large: Boolean) = if (large) largeTextMinContrast else minContrastDefault
	@deprecated("Renamed to minContrastFor", "v1.8.2")
	def minimumContrast(large: Boolean) = minContrastFor(large)
	
	/**
	  * @param fontSize Font size
	  * @param bold Whether the font is bold. Default = false.
	  * @return The minimum color contrast requirement for text with those settings, in this standard
	  */
	def minContrastForText(fontSize: Distance, bold: Boolean = false) =
		minContrastFor(large = textIsLarge(fontSize, bold))
	@deprecated("Renamed to minContrastForText", "v1.8.2")
	def minimumContrastForText(fontSize: Distance, fontIsBold: Boolean = false) =
		minContrastForText(fontSize, fontIsBold)
	
	/**
	  * Tests whether specified text settings meet these color contrast standards
	  * @param contrast Color contrast between the text and the background
	  * @param fontSize Size of the used font
	  * @param bold Whether the used font is bold (default = false)
	  * @return Whether the text meets this standard
	  */
	def acceptsText(contrast: Double, fontSize: Distance, bold: Boolean = false) = {
		// Case: Text meets higher requirements => No need to perform further checks
		if (contrast >= minContrastDefault)
			true
		// Case: Text doesn't meet the lowest requirements => No need to check further
		else if (contrast < largeTextMinContrast)
			false
		// Case: Validity is dependent on the font
		else
			contrast >= minContrastForText(fontSize, bold)
	}
	@deprecated("Renamed to acceptsText(...)", "v1.8.2")
	def test(contrast: Double, fontSize: Distance, fontIsBold: Boolean = false) =
		acceptsText(contrast, fontSize, fontIsBold)
	
	/**
	 * Tests whether a color contrast is high enough for this standard
	 * @param contrast Applicable color contrast
	 * @param large Whether the context specifies large text / shape. Default = false.
	 * @return Whether the specified color contrast is acceptable, in this standard.
	 */
	def accepts(contrast: Double, large: Boolean = false) = contrast >= minContrastFor(large)
	/**
	  * Tests a color contrast against the default contrast threshold in this standard
	  * @param contrast A color contrast
	  * @return Whether that color contrast meets the default threshold in this standard
	  */
	@deprecated("Please use accepts(...) instead", "v1.8.2")
	def test(contrast: Double) = accepts(contrast)
}