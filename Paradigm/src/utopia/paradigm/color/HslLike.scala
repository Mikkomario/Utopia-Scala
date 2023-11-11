package utopia.paradigm.color

import utopia.paradigm.angular.{Angle, NondirectionalRotation, Rotation}


/**
  * This trait represents a color value with hue, satruation and luminance
  * @author Mikko Hilpinen
  * @since Genesis 24.4.2019, v1+
  */
trait HslLike[Repr <: HslLike[Repr]]
{
	// ABSTRACT	------------------
	
	/**
	  * @return This instance
	  */
	def self: Repr
	
	/**
	  * @return The hue of this color [0, 360[ where 0 is red, 120 is green and 240 is blue
	  */
	def hue: Angle
	/**
	  * @return The saturation of this color [0, 1] where 0 is grayscale and 1 is fully saturated
	  */
	def saturation: Double
	/**
	  * @return The luminosity of this color [0, 1] where 0 is black and 1 is white
	  */
	def luminosity: Double
	
	/**
	  * @param hue New hue [0, 360[
	  * @return A copy of this color with specified hue
	  */
	def withHue(hue: Angle): Repr
	/**
	  * @param saturation New saturation [0, 1]
	  * @return A copy of this color with specified saturation
	  */
	def withSaturation(saturation: Double): Repr
	/**
	  * @param luminosity New luminosity [0, 1]
	  * @return A copy of this color with new luminosity
	  */
	def withLuminosity(luminosity: Double): Repr
	
	
	// COMPUTED	------------------
	
	/**
	  * @return The darkness factor of this color. Inverse of luminosity.
	  *         [0, 1] where 0 is totally white and 1 is totally black.
	  */
	def darkness = 1 - luminosity
	
	/**
	  * @return A percentage value of saturation [0, 100]
	  */
	def saturationPercent = (saturation * 100).toInt
	/**
	  * @return A percentage value of luminosity [0, 100]
	  */
	def luminosityPercent = (luminosity * 100).toInt
	/**
	  * @return A percentage value of darkness (i.e. inverse luminosity) [0, 100]
	  */
	def darknessPercent = (darkness * 100).toInt
	
	/**
	  * @return An angle representation of hue
	  */
	@deprecated("Simply replaced with hue", "v2.3")
	def hueAngle = hue
	
	/**
	  * @return Whether this color could be considered 'light' (luminosity above 50%)
	  */
	def isLight = luminosity > 0.5
	/**
	  * @return Whether this color could be considered 'dark' (luminosity at 50% or below)
	  */
	def isDark = luminosity <= 0.5
	
	/**
	  * @return A grayscale version of this color
	  */
	def grayscale = withSaturation(0)
	
	/**
	  * @return The complementary color of this color
	  */
	def complementary = plusHue(Rotation.clockwise.circles(0.5))
	
	
	// OPERATORS	--------------
	
	/**
	  * @param rotation Hue rotation
	  * @return A copy of this color with rotated hue
	  */
	def +(rotation: Rotation): Repr = plusHue(rotation)
	
	/**
	  * @param rotation Hue rotation
	  * @return A copy of this color with rotated hue
	  */
	def -(rotation: Rotation) = this + (-rotation)
	
	
	// OTHER	------------------
	
	/**
	  * @param darkness New darkness value to assign
	  *                 [0, 1], where 0 is white and 1 is black
	  * @return A copy of this color with that darkness value
	  */
	def withDarkness(darkness: Double) = withLuminosity(1 - darkness)
	
	/**
	  * @param amount The adjustment in hue
	  * @return A copy of this color with ajusted hue
	  */
	def plusHue(amount: Rotation) = withHue(hue + amount)
	/**
	  * Adjusts the hue of this color towards the specified target
	  * @param amount The maximum hue adjustment
	  * @param target Target hue [0, 360[
	  * @return A copy of this color with adjusted hue
	  */
	def plusHueTowards(amount: NondirectionalRotation, target: Angle) = {
		val diff = target - hue
		if (diff.isZero)
			self
		else if (diff.absolute <= amount)
			withHue(target)
		else {
			// Checks whether to go forward or backward on hue scale
			plusHue(amount.towards(diff.direction))
		}
	}
	@deprecated("Please use .plusHueTowards(NonDirectionalRotation, Angle) instead", "v1.5")
	def plusHueTowards(amountDegrees: Double, target: Angle): Repr =
		plusHueTowards(NondirectionalRotation.degrees(amountDegrees), target)
	
	/**
	  * @param amount Saturation adjustment
	  * @return A copy of this color with adjusted saturation
	  */
	def plusSaturation(amount: Double) = withSaturation(saturation + amount)
	
	/**
	  * @param amount Luminosity adjustment
	  * @return A copy of this color with adjusted luminosity
	  */
	def plusLuminosity(amount: Double) = withLuminosity(luminosity + amount)
	/**
	  * @param amount Darkness adjustment [0, 1]
	  * @return A copy of this color with adjusted darkness
	  */
	def plusDarkness(amount: Double) = minusLuminosity(amount)
	
	/**
	  * @param amount Hue adjustment
	  * @return A copy of this color with adjusted hue
	  */
	def minusHue(amount: Rotation) = withHue(hue - amount)
	
	/**
	  * @param amount Saturation adjustment
	  * @return A copy of this color with adjusted saturation
	  */
	def minusSaturation(amount: Double) = withSaturation(saturation - amount)
	
	/**
	  * @param amount Luminosity adjustment
	  * @return A copy of this color with adjusted luminosity
	  */
	def minusLuminosity(amount: Double) = withLuminosity(luminosity - amount)
	/**
	  * @param amount Darkness adjustment
	  * @return A copy of this color with adjusted darkness
	  */
	def minusDarkness(amount: Double) = plusLuminosity(amount)
	
	def timesSaturation(multiplier: Double) = withSaturation(saturation * multiplier)
	
	def timesLuminosity(multiplier: Double) = withLuminosity(luminosity * multiplier)
	/**
	  * @param multiplier A modifier applied to this color's darkness
	  * @return A copy of this color with that darkness value
	  */
	def timesDarkness(multiplier: Double) = withDarkness(darkness * multiplier)
	
	def mapHue(f: Angle => Angle) = withHue(f(hue))
	
	def mapSaturation(f: Double => Double) = withSaturation(f(saturation))
	
	def mapLuminosity(f: Double => Double) = withLuminosity(f(luminosity))
	def mapDarkness(f: Double => Double) = withDarkness(f(darkness))
	
	/**
	  * @param lightMod A lightening modifier > 0 where 2 is twice as bright, 1 keeps this color as is
	  * @return A lightened version of this color
	  */
	@deprecated("Replaced with .lightenedBy and .lightened", "v1.2")
	def lightened(lightMod: Double) = {
		if (lightMod <= 0)
			withLuminosity(0)
		else {
			val o = luminosity
			withLuminosity(1 - (1 - o) / lightMod)
		}
	}
	/**
	  * @param darkMod A darkening modifier > 0 where 2 is twice as dark, 1 keeps this color as is
	  * @return A darkened version of this color
	  */
	@deprecated("Replaced with .darkenedBy and .darkened", "v1.2")
	def darkened(darkMod: Double) = {
		if (darkMod <= 0)
			withLuminosity(1)
		else
			timesLuminosity(1 / darkMod)
	}
}
