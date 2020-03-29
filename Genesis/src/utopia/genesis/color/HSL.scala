package utopia.genesis.color

import scala.language.implicitConversions
import utopia.genesis.shape.Angle
import utopia.genesis.util.ApproximatelyEquatable
import utopia.genesis.util.Extensions._

object HSL
{
	// IMPLICIT	--------------------
	
	/**
	  * Implicitly converts a hsl color to color
	  * @param hsl A hsl color
	  * @return A color
	  */
	implicit def hslToColor(hsl: HSL): Color = Color(Left(hsl), 1.0)
	
	
	// OPERATORS	----------------
	
	/**
	  * Creates a new HSL color
	  * @param hue Color hue [0, 360[ where 0 is red, 120 is green and 240 is blue
	  * @param saturation Saturation [0, 1] where 0 is grayscale and 1 is fully saturated
	  * @param luminosity Luminosity [0, 1] where 0 is black and 1 is white
	  * @return A new HSL color
	  */
	def apply(hue: Double, saturation: Double, luminosity: Double): HSL =
	{
		val hue2 = hue % 360
		val h = if (hue2 < 0) hue2 + 360 else hue2
		val s = 0.0 max saturation min 1.0
		val l = 0.0 max luminosity min 1.0
		
		new HSL(h, s, l)
	}
	
	/**
	  * Creates a new HSL color
	  * @param hueAngle Hue angle [0, 360[ degrees, where 0 is red, 120 is green and 240 is blue
	  * @param saturation Saturation [0, 1] where 0 is grayscale and 1 is fully saturated
	  * @param luminosity Luminosity [0, 1] where 0 is black and 1 is white
	  * @return A new HSL color
	  */
	def apply(hueAngle: Angle, saturation: Double, luminosity: Double): HSL = apply(hueAngle.toDegrees, saturation, luminosity)
}

/**
  * A HSL represents a color value with hue, satruation and luminance
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  * @param hue Color hue [0, 360[ where 0 is red, 120 is green and 240 is blue
  * @param saturation Color saturation [0, 1] where 0 is grayscale and 1 is fully saturated
  * @param luminosity Color luminosity [0, 1] where 0 is black and 1 is white
  */
case class HSL private(override val hue: Double, override val saturation: Double, override val luminosity: Double)
	extends HSLLike[HSL] with ApproximatelyEquatable[HSLLike[_]]
{
	// COMPUTED	------------------
	
	/**
	  * @return An RGB representation of this color
	  */
	def toRGB =
	{
		//  Formula needs all values between 0 - 1.
		val h = hue / 360
		
		val q =
		{
			if (luminosity < 0.5)
				luminosity * (1 + saturation)
			else
				(luminosity + saturation) - (saturation * luminosity)
		}
		
		val p = 2 * luminosity - q
		
		val r = hueToRGB(p, q, h + (1.0 / 3.0))
		val g = hueToRGB(p, q, h)
		val b = hueToRGB(p, q, h - (1.0 / 3.0))
		
		RGB(r, g, b)
	}
	
	
	// IMPLEMENTED	--------------
	
	/**
	  * Checks whether the two instances are approximately equal
	  */
	override def ~==(other: HSLLike[_]) = (hue ~== other.hue) &&
		(saturation ~== other.saturation) && (luminosity ~== other.luminosity)
	
	/**
	  * @param hue New hue [0, 360[
	  * @return A copy of this color with new hue
	  */
	def withHue(hue: Double) = HSL.apply(hue, saturation, luminosity)
	
	/**
	  * @param saturation New saturation [0, 1]
	  * @return A copy of this color with new saturation
	  */
	def withSaturation(saturation: Double) = HSL.apply(hue, saturation, luminosity)
	
	/**
	  * @param luminosity New luminosity [0, 1]
	  * @return A copy of this color with new luminosity
	  */
	def withLuminosity(luminosity: Double) = HSL.apply(hue, saturation, luminosity)
	
	override def toString = s"Hue: $hue, Saturation: $saturationPercent%, Luminosity: $luminosityPercent%"
	
	
	// OPERATORS	--------------
	
	/**
	  * @param amount Hue adjust
	  * @return A copy of this color with adjusted hue
	  */
	def +(amount: Double) = plusHue(amount)
	
	/**
	  * @param amount Hue adjust
	  * @return A copy of this color with adjusted hue
	  */
	def -(amount: Double) = minusHue(amount)
	
	
	// OTHER	------------------
	
	private def hueToRGB(p: Double, q: Double, h0: Double) =
	{
		// Sets h to range 0-1
		val h = if (h0 < 0) h0 + 1 else if (h0 > 1) h0 - 1 else h0
		
		if (6 * h < 1)
			p + ((q - p) * 6 * h)
		else if (2 * h < 1 )
			q
		else if (3 * h < 2)
			p + ( (q - p) * 6 * ((2.0 / 3.0) - h) )
		else
			p
	}
}
