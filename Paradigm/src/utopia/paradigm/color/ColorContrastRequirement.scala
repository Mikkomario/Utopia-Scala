package utopia.paradigm.color

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.ordering.SelfComparable
import utopia.paradigm.color.ColorContrastRequirement.textIsLarge
import utopia.paradigm.measurement.Distance
import utopia.paradigm.transform.LinearSizeAdjustable

object ColorContrastRequirement
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
	
	
	// COMPUTED -----------------------------
	
	@deprecated("Renamed to largeTextSizeThreshold", "v1.9")
	def largeTextThreshold = largeTextSizeThreshold
	@deprecated("Renamed to largeTextSizeThresholdForBold", "v1.9")
	def largeTextThresholdBold = largeTextSizeThresholdForBold
	
	
	// OTHER	-----------------------------
	
	/**
	 * @param minContrastDefault Minimum contrast ratio to use by default
	 * @return Color contrast standard applying the specified limits
	 */
	def apply(minContrastDefault: Double): ColorContrastRequirement = apply(minContrastDefault, minContrastDefault / 1.5)
	/**
	 * @param minContrastDefault Minimum contrast ratio to use by default
	 * @param largeTextMinContrast Minimum contrast ratio applied to large text & other large surfaces
	 * @return Color contrast standard applying the specified limits
	 */
	def apply(minContrastDefault: Double, largeTextMinContrast: Double): ColorContrastRequirement =
		new _ColorContrastRequirement(minContrastDefault, largeTextMinContrast)
	
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
	
	private class _ColorContrastRequirement(override val defaultMin: Double,
	                                        override val largeTextMin: Double)
		extends ColorContrastRequirement
	{
		override def self: ColorContrastRequirement = this
	}
}

/**
  * Determines color contrast thresholds
  * @author Mikko Hilpinen
  * @since 09.09.2026, v1.9
  * @see https://webaim.org/articles/contrast/
  */
trait ColorContrastRequirement
	extends SelfComparable[ColorContrastRequirement] with EqualsBy with LinearSizeAdjustable[ColorContrastRequirement]
		// Extends a wrapper trait so that this trait can be directly used as an implicit parameter
		with HasColorContrastRequirements
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return The minimum contrast ratio that is normally used
	  */
	def defaultMin: Double
	/**
	  * @return The minimum contrast ratio that is allowed for large text
	  */
	def largeTextMin: Double
	
	
	// COMPUTED ---------------------------
	
	@deprecated("Renamed to defaultMin", "v1.9")
	def defaultMinimumContrast: Double = defaultMin
	@deprecated("Renamed to largeTextMin", "v1.9")
	def largeTextMinimumContrast: Double = largeTextMin
	
	
	// IMPLEMENTED  -----------------------
	
	override def requiredContrast: ColorContrastRequirement = self
	
	override protected def equalsProperties: IterableOnce[Any] = Pair(defaultMin, largeTextMin)
	
	override def compareTo(o: ColorContrastRequirement): Int = {
		if (defaultMin == o.defaultMin)
			largeTextMin.compareTo(o.largeTextMin)
		else
			defaultMin.compareTo(o.defaultMin)
	}
	
	override def *(mod: Double): ColorContrastRequirement =
		ColorContrastRequirement(defaultMin * mod, largeTextMin * mod)
	
	
	// OTHER	---------------------------
	
	/**
	  * @param large Whether the viewed object or text is large (true) or small (false)
	  * @return Minimum contrast for objects of that size
	  */
	def minFor(large: Boolean) = if (large) largeTextMin else defaultMin
	@deprecated("Renamed to minFor", "v1.9")
	def minimumContrast(large: Boolean) = minFor(large)
	
	/**
	  * @param fontSize Font size
	  * @param bold Whether the font is bold. Default = false.
	  * @return The minimum color contrast requirement for text with those settings, in this standard
	  */
	def minForText(fontSize: Distance, bold: Boolean = false) = minFor(large = textIsLarge(fontSize, bold))
	@deprecated("Renamed to minForText", "v1.9")
	def minimumContrastForText(fontSize: Distance, fontIsBold: Boolean = false) =
		minForText(fontSize, fontIsBold)
	
	/**
	  * Tests whether specified text settings meet these color contrast standards
	  * @param contrast Color contrast between the text and the background
	  * @param fontSize Size of the used font
	  * @param bold Whether the used font is bold (default = false)
	  * @return Whether the text meets this standard
	  */
	def acceptsText(contrast: Double, fontSize: Distance, bold: Boolean = false) = {
		// Case: Text meets higher requirements => No need to perform further checks
		if (contrast >= defaultMin)
			true
		// Case: Text doesn't meet the lowest requirements => No need to check further
		else if (contrast < largeTextMin)
			false
		// Case: Validity is dependent on the font
		else
			contrast >= minForText(fontSize, bold)
	}
	@deprecated("Renamed to acceptsText(...)", "v1.9")
	def test(contrast: Double, fontSize: Distance, fontIsBold: Boolean = false) =
		acceptsText(contrast, fontSize, fontIsBold)
	
	/**
	 * Checks whether a color contrast is high enough to be used on a larger surface element
	 * @param contrast Color contrast to test
	 * @return Whether the specified contrast is high enough to be used on larger surfaces
	 */
	def acceptsSurface(contrast: Double) = accepts(contrast, large = true)
	
	/**
	 * Tests whether a color contrast is high enough for this standard
	 * @param contrast Applicable color contrast
	 * @param large Whether the context specifies large text / shape. Default = false.
	 * @return Whether the specified color contrast is acceptable, in this standard.
	 */
	def accepts(contrast: Double, large: Boolean = false) = contrast >= minFor(large)
	/**
	  * Tests a color contrast against the default contrast threshold in this standard
	  * @param contrast A color contrast
	  * @return Whether that color contrast meets the default threshold in this standard
	  */
	@deprecated("Please use accepts(...) instead", "v1.9")
	def test(contrast: Double) = accepts(contrast)
}