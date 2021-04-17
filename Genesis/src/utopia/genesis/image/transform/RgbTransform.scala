package utopia.genesis.image.transform

import utopia.genesis.color.Color

/**
  * This filter transforms RGB color channel values
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  */
trait RgbTransform extends PixelTransform
{
	// ABSTRACT	------------------
	
	/**
	  * Transforms a single rbg ratio
	  * @param originalRatio The original rgb ratio [0, 1]
	  * @return The new rgb ratio [0, 1]
	  */
	def apply(originalRatio: Double): Double
	
	
	// IMPLEMENTED	--------------
	
	override def apply(original: Color) = original.mapRatios(apply)
	
	
	// OPERATORS	--------------
	
	/**
	  * Combines these two RGB transformations
	  * @param another Another transformation
	  * @return A transformation that applies 'this' first and then 'another'
	  */
	def +(another: RgbTransform): RgbTransform = AndRgbTransform(this, another)
}

private case class AndRgbTransform(first: RgbTransform, second: RgbTransform) extends RgbTransform
{
	override def apply(originalRatio: Double) = second(first(originalRatio))
}