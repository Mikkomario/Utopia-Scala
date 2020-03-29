package utopia.genesis.color

import scala.language.implicitConversions
import utopia.genesis.color.RGBChannel._
import utopia.genesis.util.ApproximatelyEquatable
import utopia.genesis.util.Extensions._

import scala.collection.immutable.HashMap

object RGB
{
	// ATTRIBUTES	-------------------
	
	/**
	  * Maximum for 'value'
	  */
	val maxValue = 255
	
	
	// IMPLICIT	-----------------------
	
	/**
	  * Implicitly converts an rgbColor to a color
	  * @param rgb An rgb color
	  * @return A color
	  */
	implicit def rgbToColor(rgb: RGB): Color = Color(Right(rgb), 1.0)
	
	
	// OPERATORS	-------------------
	
	/**
	  * Creates a new RGB color
	  * @param r Red ratio [0, 1]
	  * @param g Green ratio [0, 1]
	  * @param b Blue ratio [0, 1]
	  * @return A new RGB color
	  */
	def apply(r: Double, g: Double, b: Double) = withRatios(HashMap(Red -> r, Green -> g, Blue -> b))
	
	/**
	  * Creates a single channel RGB color
	  * @param channel Color channel
	  * @param ratio Color ratio [0, 1]
	  * @return A new RGB color
	  */
	def apply(channel: RGBChannel, ratio: Double) = new RGB(HashMap(channel -> ratio))
	
	
	// OTHER	----------------------
	
	/**
	  * Creates a red color
	  * @param ratio Ratio / saturation [0, 1]
	  * @return A new RGB Color
	  */
	def red(ratio: Double) = RGB(Red, ratio)
	
	/**
	  * Creates a green color
	  * @param ratio Ratio / saturation [0, 1]
	  * @return A new RGB Color
	  */
	def green(ratio: Double) = RGB(Green, ratio)
	
	/**
	  * Creates a blue color
	  * @param ratio Ratio / saturation [0, 1]
	  * @return A new RGB Color
	  */
	def blue(ratio: Double) = RGB(Blue, ratio)
	
	/**
	  * Creates a grayscale color
	  * @param luminosity luminosity [0, 1], where 0 is black and 1 is white
	  * @return A new RGB Color
	  */
	def gray(luminosity: Double) = withRatios(RGBChannel.values.map { _ -> luminosity }.toMap)
	
	/**
	 * Creates a grayscale color
	 * @param value Color value [0, 255] where 0 is black and 255 is white
	 * @return A new RGB color
	 */
	def grayWithValue(value: Int) = gray(value / 255.0)
	
	/**
	  * Creates a new RGB with specified color ratios
	  * @param ratios Ratios per channel [0, 1]
	  * @return A new RGB color
	  */
	def withRatios(ratios: Map[RGBChannel, Double]) = new RGB(ratios.mapValues(inRange).view.force)
	
	/**
	  * Creates a new RGB with color values
	  * @param values Values per channel [0, 255]
	  * @return A new RGB color
	  */
	def withValues(values: Map[RGBChannel, Int]) = withRatios(values.mapValues { _.toDouble / maxValue })
	
	/**
	  * Creates a new RGB with color values
	  * @param r Red value [0, 255]
	  * @param g Green value [0, 255]
	  * @param b Blue value [0, 255]
	  * @return A new RGB color
	  */
	def withValues(r: Int, g: Int, b: Int): RGB = withValues(HashMap(Red -> r, Green -> g, Blue -> b))
	
	private def inRange(ratio: Double) = 0.0 max ratio min 1.0
}

/**
  * An RGB represents a color with red, blue and green
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
case class RGB private(override val ratios: Map[RGBChannel, Double]) extends RGBLike[RGB] with ApproximatelyEquatable[RGBLike[_]]
{
	// COMPUTED	------------------------
	
	/**
	  * @return A HSL representation of this RGB color
	  */
	def toHSL =
	{
		val r = red
		val g = green
		val b = blue
		
		val min = minRatio
		val max = maxRatio
		
		val hue =
		{
			if (max == min)
				0.0
			else if (max == r)
				((60 * (g - b) / (max - min)) + 360) % 360
			else if (max == g)
				(60 * (b - r) / (max - min)) + 120
			else
				(60 * (r - g) / (max - min)) + 240
		}
		
		val luminosity = (max + min) / 2
		
		val saturation =
		{
			if (max == min)
				0
			else if (luminosity <= 0.5)
				(max - min) / (max + min)
			else
				(max - min) / (2 - max - min)
		}
		
		HSL(hue, saturation, luminosity)
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def ~==(other: RGBLike[_]) = RGBChannel.values.forall { c => ratio(c) ~== other.ratio(c) }
	
	override def withRatios(newRatios: Map[RGBChannel, Double]) = RGB.withRatios(newRatios)
	
	override def toString = s"R: ${percent(Red)}%, G: ${percent(Green)}%, B: ${percent(Blue)}%"
	
	
	// OPERATORS	--------------------
	
	/**
	  * Finds the ratio for a single color channel
	  * @param channel A color channel
	  * @return The color ratio for that channel [0, 1]
	  */
	def apply(channel: RGBChannel): Double = ratios.getOrElse(channel, 0)
	
	/**
	  * Multiplies the color values in this color
	  * @param multiplier A multiplier
	  * @return A multiplied version of this color
	  */
	def *(multiplier: Double) = RGB.withRatios(ratios.mapValues { _ * multiplier })
	
	/**
	  * Divides the color values in this color
	  * @param div A divider
	  * @return A divided version of this color
	  */
	def /(div: Double) = this * (1/div)
	
	
	// OTHER	------------------------
	
	/**
	  * @param another Another RGB
	  * @return A minimum between these two colors on each RGB channel
	  */
	def min(another: RGB) = mergeWith(another, _ min _)
	/**
	  * @param another Another RGB
	  * @return A maximum between these two colors on each RGB channel
	  */
	def max(another: RGB) = mergeWith(another, _ max _)
	/**
	  * @param another Another RGB
	  * @return An average between these two colors on each RGB channel
	  */
	def average(another: RGB) = mergeWith(another, (a, b) => (a + b) / 2)
	
	private def mergeWith(another: RGB, f: (Double, Double) => Double) = RGB.withRatios(
		RGBChannel.values.map { c => c -> f(apply(c), another(c)) }.toMap)
}
