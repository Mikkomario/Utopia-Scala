package utopia.genesis.color

import utopia.genesis.shape.shape1D.{Angle, Rotation}


/**
  * This trait represents a color value with hue, satruation and luminance
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
trait HslLike[Repr <: HslLike[Repr]]
{
	// ABSTRACT	------------------
	
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
	  * @return A percentage value of saturation [0, 100]
	  */
	def saturationPercent = (saturation * 100).toInt
	/**
	  * @return A percentage value of luminosity [0, 100]
	  */
	def luminosityPercent = (luminosity * 100).toInt
	
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
	def complementary = plusHue(Rotation.ofCircles(0.5))
	
	
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
	  * @param amount The adjustment in hue
	  * @return A copy of this color with ajusted hue
	  */
	def plusHue(amount: Rotation) = withHue(hue + amount)
	
	/**
	  * Adjusts the hue of this color towards the specified target
	  * @param amountDegrees The maximum hue adjustment
	  * @param target Target hue [0, 360[
	  * @return A copy of this color with adjusted hue
	  */
	def plusHueTowards(amountDegrees: Double, target: Angle) =
	{
		val diff = target - hue
		if (diff.isZero)
			this
		else if (diff.degrees <= amountDegrees)
			withHue(target)
		else
		{
			// Checks whether to go forward or backward on hue scale
			plusHue(Rotation.ofDegrees(amountDegrees, diff.direction))
		}
	}
	
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
	
	def timesSaturation(multiplier: Double) = withSaturation(saturation * multiplier)
	
	def timesLuminosity(multiplier: Double) = withLuminosity(luminosity * multiplier)
	
	def mapHue(f: Angle => Angle) = withHue(f(hue))
	
	def mapSaturation(f: Double => Double) = withSaturation(f(saturation))
	
	def mapLuminosity(f: Double => Double) = withLuminosity(f(luminosity))
	
	/**
	  * @param lightMod A lightening modifier > 0 where 2 is twice as bright, 1 keeps this color as is
	  * @return A lightened version of this color
	  */
	def lightened(lightMod: Double) =
	{
		if (lightMod <= 0)
			withLuminosity(0)
		else
		{
			val o = luminosity
			withLuminosity(1 - (1 - o) / lightMod)
		}
	}
	
	/**
	  * @param darkMod A darkening modifier > 0 where 2 is twice as dark, 1 keeps this color as is
	  * @return A darkened version of this color
	  */
	def darkened(darkMod: Double) =
	{
		if (darkMod <= 0)
			withLuminosity(1)
		else
			timesLuminosity(1 / darkMod)
	}
}
