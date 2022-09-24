package utopia.paradigm.color

import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.enumeration.RgbChannel
import utopia.paradigm.enumeration.RgbChannel.{Blue, Green, Red}

import scala.math.Ordering.Double.TotalOrdering

/**
  * This trait represents a color with red, blue and green
  * @author Mikko Hilpinen
  * @since Genesis 24.4.2019, v1+
  */
trait RgbLike[Repr <: RgbLike[Repr]]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return The rgb color channel ratios of this color
	  */
	def ratios: Map[RgbChannel, Double]
	
	/**
	  * @param newRatios A new set of color ratios
	  * @return A copy of this color with specified ratios
	  */
	def withRatios(newRatios: Map[RgbChannel, Double]): Repr
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return The red component of this color [0, 1]
	  */
	def red = ratio(Red)
	
	/**
	  * @return The green component of this color [0, 1]
	  */
	def green = ratio(Green)
	
	/**
	  * @return The blue component of this color [0, 1]
	  */
	def blue = ratio(Blue)
	
	/**
	  * @return The red value of this color [0, 255]
	  */
	def redValue = value(Red)
	
	/**
	  * @return The green value of this color [0, 255]
	  */
	def greenValue = value(Green)
	
	/**
	  * @return The blue value of this color [0, 255]
	  */
	def blueValue = value(Blue)
	
	/**
	  * @return The color values of this RGB [0, 255] each
	  */
	def values = ratios.view.mapValues { r => (r * Rgb.maxValue).toInt }.toMap
	
	/**
	  * @return An inverted version of this RGB (where black is white)
	  */
	def inverted = withRatios(ratios.view.mapValues { 1 - _ }.toMap)
	
	/**
	  * @return The top ratio of this color's channels [0, 1]
	  */
	def maxRatio = ratios.values.max
	
	/**
	  * @return The minimum ratio of this color's channels [0, 1]
	  */
	def minRatio = RgbChannel.values.map(ratio).min
	
	/**
	  * @return The relative luminance of this color, which is used in contrast calculations, for example
	  * @see https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef
	  */
	def relativeLuminance =
	{
		// Calculation based on:
		// https://stackoverflow.com/questions/61525100/convert-from-relative-luminance-to-hsl
		// Each color channel has its own multiplier
		Map(Red -> 0.2126, Green -> 0.7152, Blue -> 0.0722).map { case (channel, multiplier) =>
			// Calculates the relative luminance value for each channel and applies the multiplier
			val ratio = this.ratio(channel)
			val gammaAdjusted = if (ratio <= 0.03928) ratio / 12.92 else math.pow((ratio + 0.055) / 1.055, 2.4)
			gammaAdjusted * multiplier
		}.sum // The returned luminance is the sum of the individual channel values (with multipliers applied)
	}
	
	
	// OPERATORS	--------------------
	
	/**
	  * Combines this RGB with another by adding the color values together
	  * @param other Another RGB
	  * @return A combination of these two RGB's
	  */
	def +(other: Rgb) = withRatios(ratios.mergeWith(other.ratios) { _ + _ })
	
	/**
	  * Combines this RGB with another by subtracting the color values
	  * @param other Another RGB
	  * @return A subtraction of these RGB's
	  */
	def -(other: Rgb) = withRatios(ratios.map { case (k, v) => k -> (v - other(k)) })
	
	
	// OTHER	------------------------
	
	/**
	  * Finds a color ratio for a color channel
	  * @param channel Target color channel
	  * @return Color ratio for that channel [0, 1]
	  */
	def ratio(channel: RgbChannel): Double = ratios.getOrElse(channel, 0)
	
	/**
	  * Finds a color value for a color channel
	  * @param channel Target color channel
	  * @return Color value for that channel[0, 255]
	  */
	def value(channel: RgbChannel) = (ratio(channel) * Rgb.maxValue).toInt
	
	/**
	  * Finds a color saturation % for a channel
	  * @param channel Target channel
	  * @return Color saturation % [0, 100]
	  */
	def percent(channel: RgbChannel) = (ratio(channel) * 100).toInt
	
	/**
	  * Creates a copy of this RGB with modified color ratio
	  * @param channel Target channel
	  * @param ratio New color ratio [0, 1]
	  * @return A new RGB
	  */
	def withRatio(channel: RgbChannel, ratio: Double) = withRatios(ratios + (channel -> ratio))
	
	/**
	  * Creates a copy of this RGB with modified color value
	  * @param channel Target channel
	  * @param value New color value [0, 255]
	  * @return A new RGB
	  */
	def withValue(channel: RgbChannel, value: Int) = withRatio(channel, value / Rgb.maxValue.toDouble)
	
	/**
	  * Creates a copy of this RGB with modified red color ratio
	  * @param ratio New color ratio [0, 1]
	  * @return A new RGB
	  */
	def withRed(ratio: Double) = withRatio(Red, ratio)
	
	/**
	  * Creates a copy of this RGB with modified green color ratio
	  * @param ratio New color ratio [0, 1]
	  * @return A new RGB
	  */
	def withGreen(ratio: Double) = withRatio(Green, ratio)
	
	/**
	  * Creates a copy of this RGB with modified blue color ratio
	  * @param ratio New color ratio [0, 1]
	  * @return A new RGB
	  */
	def withBlue(ratio: Double) = withRatio(Blue, ratio)
	
	/**
	  * @param f A mapping function for single rgb ratios
	  * @return A copy of this color with mapped rgb ratios
	  */
	def mapRatios(f: Double => Double) = withRatios(ratios.view.mapValues(f).toMap)
	
	
	// OTHER	------------------------------
	
	/**
	  * Calculates the contrast between these two colors
	  * @param other Another color
	  * @return The color contrast ratio between these two colors
	  * @see https://webaim.org/articles/contrast/
	  */
	def contrastAgainst(other: RgbLike[_]) =
	{
		// Calculation from: https://stackoverflow.com/questions/61525100/convert-from-relative-luminance-to-hsl
		// Compares the relative luminosities of these two colors
		val ratio = (relativeLuminance + 0.05) / (other.relativeLuminance + 0.05)
		ColorContrast(if (ratio >= 1) ratio else 1.0 / ratio)
	}
}
