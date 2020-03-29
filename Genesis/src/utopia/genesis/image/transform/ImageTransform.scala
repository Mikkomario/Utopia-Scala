package utopia.genesis.image.transform

import utopia.genesis.image.Image

/**
  * These filters are used for transforming image data
  * @author Mikko Hilpinen
  * @since 15.6.2019, v2.1+
  */
trait ImageTransform
{
	// ABSTRACT	-------------------
	
	/**
	  * Transforms an image
	  * @param source The source image
	  * @return The transformed image
	  */
	def apply(source: Image): Image
	
	
	// OPERATORS	---------------
	
	/**
	  * Combines two image filters
	  * @param another Another image filter
	  * @return A combination of these filters where the second filter is applied after the first one
	  */
	def +(another: ImageTransform): ImageTransform = AndImageTransform(this, another)
}

private case class AndImageTransform(first: ImageTransform, second: ImageTransform) extends ImageTransform
{
	override def apply(source: Image) = second(first(source))
}
