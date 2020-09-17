package utopia.genesis.color

import scala.language.implicitConversions
import utopia.genesis.color.RgbChannel._
import utopia.genesis.shape.shape1D.Angle
import utopia.genesis.util.ApproximatelyEquatable
import utopia.genesis.util.Extensions._

import scala.collection.immutable.HashMap

object Rgb
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
	implicit def rgbToColor(rgb: Rgb): Color = Color(Right(rgb), 1.0)
	
	
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
	def apply(channel: RgbChannel, ratio: Double) = new Rgb(HashMap(channel -> ratio))
	
	
	// OTHER	----------------------
	
	/**
	  * Creates a red color
	  * @param ratio Ratio / saturation [0, 1]
	  * @return A new RGB Color
	  */
	def red(ratio: Double) = Rgb(Red, ratio)
	
	/**
	  * Creates a green color
	  * @param ratio Ratio / saturation [0, 1]
	  * @return A new RGB Color
	  */
	def green(ratio: Double) = Rgb(Green, ratio)
	
	/**
	  * Creates a blue color
	  * @param ratio Ratio / saturation [0, 1]
	  * @return A new RGB Color
	  */
	def blue(ratio: Double) = Rgb(Blue, ratio)
	
	/**
	  * Creates a grayscale color
	  * @param luminosity luminosity [0, 1], where 0 is black and 1 is white
	  * @return A new RGB Color
	  */
	def gray(luminosity: Double) = withRatios(RgbChannel.values.map { _ -> luminosity }.toMap)
	
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
	def withRatios(ratios: Map[RgbChannel, Double]) = new Rgb(ratios.view.mapValues(inRange).toMap)
	
	/**
	  * Creates a new RGB with color values
	  * @param values Values per channel [0, 255]
	  * @return A new RGB color
	  */
	def withValues(values: Map[RgbChannel, Int]) = withRatios(values.view.mapValues { _.toDouble / maxValue }.toMap)
	
	/**
	  * Creates a new RGB with color values
	  * @param r Red value [0, 255]
	  * @param g Green value [0, 255]
	  * @param b Blue value [0, 255]
	  * @return A new RGB color
	  */
	def withValues(r: Int, g: Int, b: Int): Rgb = withValues(HashMap(Red -> r, Green -> g, Blue -> b))
	
	private def inRange(ratio: Double) = 0.0 max ratio min 1.0
}

/**
  * An RGB represents a color with red, blue and green
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
// TODO: Rename to Rgb (same with hsl)
case class Rgb private(override val ratios: Map[RgbChannel, Double]) extends RgbLike[Rgb] with ApproximatelyEquatable[RgbLike[_]]
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
		
		Hsl(Angle.ofDegrees(hue), saturation, luminosity)
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def ~==(other: RgbLike[_]) = RgbChannel.values.forall { c => ratio(c) ~== other.ratio(c) }
	
	override def withRatios(newRatios: Map[RgbChannel, Double]) = Rgb.withRatios(newRatios)
	
	override def toString = s"R: ${percent(Red)}%, G: ${percent(Green)}%, B: ${percent(Blue)}%"
	
	
	// OPERATORS	--------------------
	
	/**
	  * Finds the ratio for a single color channel
	  * @param channel A color channel
	  * @return The color ratio for that channel [0, 1]
	  */
	def apply(channel: RgbChannel): Double = ratios.getOrElse(channel, 0)
	
	/**
	  * Multiplies the color values in this color
	  * @param multiplier A multiplier
	  * @return A multiplied version of this color
	  */
	def *(multiplier: Double) = Rgb.withRatios(ratios.view.mapValues { _ * multiplier }.toMap)
	
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
	def min(another: Rgb) = mergeWith(another) { _ min _ }
	/**
	  * @param another Another RGB
	  * @return A maximum between these two colors on each RGB channel
	  */
	def max(another: Rgb) = mergeWith(another) { _ max _ }
	/**
	  * @param another Another RGB
	  * @return An average between these two colors on each RGB channel
	  */
	def average(another: Rgb) = mergeWith(another) { (a, b) => (a + b) / 2 }
	
	/**
	  * @param another Another RGB
	  * @param weight Weight modifier assigned to THIS color
	  * @return An average between these two colors on each RGB channel
	  */
	def average(another: Rgb, weight: Double) = mergeWith(another) { (a, b) => (a * weight + b) / (1 + weight) }
	
	/**
	  * @param another Another RGB
	  * @param myWeight Weight modifier assigned to THIS color
	  * @param theirWeight Weight modifier assigned to specified color
	  * @return An average between these two colors on each RGB channel
	  */
	def average(another: Rgb, myWeight: Double, theirWeight: Double): Rgb = average(another, myWeight / theirWeight)
	
	private def mergeWith(another: Rgb)(f: (Double, Double) => Double) = Rgb.withRatios(
		RgbChannel.values.map { c => c -> f(apply(c), another(c)) }.toMap)
}
