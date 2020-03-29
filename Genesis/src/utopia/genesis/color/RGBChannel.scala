package utopia.genesis.color

object RGBChannel
{
	/**
	  * The red channel
	  */
	case object Red extends RGBChannel
	/**
	  * The green channel
	  */
	case object Green extends RGBChannel
	/**
	  * The blue channel
	  */
	case object Blue extends RGBChannel
	
	/**
	  * All of the RGB channels
	  */
	val values: Vector[RGBChannel] = Vector(Red, Green, Blue)
}

/**
  * These are the different channels an RGB color will use
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
sealed trait RGBChannel extends Equals
{
	/**
	  * Converts this channel into a single-channeled RGB value
	  * @param ratio The color ratio [0, 1] where 0 is black and 1 is fully saturated
	  * @return A new RGB color
	  */
	def apply(ratio: Double) = RGB(this, ratio)
}