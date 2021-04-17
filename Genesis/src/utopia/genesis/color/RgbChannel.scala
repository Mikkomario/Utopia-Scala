package utopia.genesis.color

object RgbChannel
{
	/**
	  * The red channel
	  */
	case object Red extends RgbChannel
	/**
	  * The green channel
	  */
	case object Green extends RgbChannel
	/**
	  * The blue channel
	  */
	case object Blue extends RgbChannel
	
	/**
	  * All of the RGB channels
	  */
	val values: Vector[RgbChannel] = Vector(Red, Green, Blue)
}

/**
  * These are the different channels an RGB color will use
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
sealed trait RgbChannel extends Equals
{
	/**
	  * Converts this channel into a single-channeled RGB value
	  * @param ratio The color ratio [0, 1] where 0 is black and 1 is fully saturated
	  * @return A new RGB color
	  */
	def apply(ratio: Double) = Rgb(this, ratio)
}