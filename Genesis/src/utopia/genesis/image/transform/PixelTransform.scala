package utopia.genesis.image.transform

import utopia.genesis.color.Color
import utopia.genesis.image.Image

/**
  * This filter transforms images by transforming each pixel color values
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1+
  */
trait PixelTransform extends ImageTransform
{
	// ABSTRACT	---------------------
	
	/**
	  * Maps a single pixel color value
	  * @param original The original pixel color
	  * @return The mapped pixel color
	  */
	def apply(original: Color): Color
	
	
	// IMPLEMENTED	----------------
	
	override def apply(source: Image) = source.mapPixels(apply)
	
	
	// OPERATORS	----------------
	
	/**
	  * Combines this transformation with another so that both are applied
	  * @param another Another transformation
	  * @return A transformation that first applies this one and then the another one
	  */
	def +(another: PixelTransform): PixelTransform = AndPixelTransform(this, another)
}

private case class AndPixelTransform(first: PixelTransform, second: PixelTransform) extends PixelTransform
{
	override def apply(original: Color) = second(first(original))
}