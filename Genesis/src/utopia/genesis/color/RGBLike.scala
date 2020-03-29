package utopia.genesis.color

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.color.RGBChannel._

/**
  * This trait represents a color with red, blue and green
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
trait RGBLike[Repr <: RGBLike[Repr]]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return The rgb color channel ratios of this color
	  */
	def ratios: Map[RGBChannel, Double]
	
	/**
	  * @param newRatios A new set of color ratios
	  * @return A copy of this color with specified ratios
	  */
	def withRatios(newRatios: Map[RGBChannel, Double]): Repr
	
	
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
	def values = ratios.mapValues { r => (r * RGB.maxValue).toInt }
	
	/**
	  * @return An inverted version of this RGB (where black is white)
	  */
	def inverted = withRatios(ratios.mapValues { 1 - _ })
	
	/**
	  * @return The top ratio of this color's channels [0, 1]
	  */
	def maxRatio = ratios.values.max
	
	/**
	  * @return The minimum ratio of this color's channels [0, 1]
	  */
	def minRatio = RGBChannel.values.map(ratio).min
	
	
	// OPERATORS	--------------------
	
	/**
	  * Combines this RGB with another by adding the color values together
	  * @param other Another RGB
	  * @return A combination of these two RGB's
	  */
	def +(other: RGB) = withRatios(ratios.mergedWith(other.ratios, _ + _))
	
	/**
	  * Combines this RGB with another by subtracting the color values
	  * @param other Another RGB
	  * @return A subtraction of these RGB's
	  */
	def -(other: RGB) = withRatios(ratios.map { case (k, v) => k -> (v - other(k)) })
	
	
	// OTHER	------------------------
	
	/**
	  * Finds a color ratio for a color channel
	  * @param channel Target color channel
	  * @return Color ratio for that channel [0, 1]
	  */
	def ratio(channel: RGBChannel): Double = ratios.getOrElse(channel, 0)
	
	/**
	  * Finds a color value for a color channel
	  * @param channel Target color channel
	  * @return Color value for that channel[0, 255]
	  */
	def value(channel: RGBChannel) = (ratio(channel) * RGB.maxValue).toInt
	
	/**
	  * Finds a color saturation % for a channel
	  * @param channel Target channel
	  * @return Color saturation % [0, 100]
	  */
	def percent(channel: RGBChannel) = (ratio(channel) * 100).toInt
	
	/**
	  * Creates a copy of this RGB with modified color ratio
	  * @param channel Target channel
	  * @param ratio New color ratio [0, 1]
	  * @return A new RGB
	  */
	def withRatio(channel: RGBChannel, ratio: Double) = withRatios(ratios + (channel -> ratio))
	
	/**
	  * Creates a copy of this RGB with modified color value
	  * @param channel Target channel
	  * @param value New color value [0, 255]
	  * @return A new RGB
	  */
	def withValue(channel: RGBChannel, value: Int) = withRatio(channel, value / RGB.maxValue.toDouble)
	
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
	def mapRatios(f: Double => Double) = withRatios(ratios.mapValues(f))
}
