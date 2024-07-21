package utopia.echo.model.request

import utopia.flow.collection.immutable.Single

/**
  * Common trait for items that allow attachment of Base64 encoded images
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait CanAttachImages[+Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param base64EncodedImages Images to attach to this item.
	  *                            Specify all images as Base 64 encoded strings.
	  * @return Copy of this ite with the specified images attached
	  */
	def attachImages(base64EncodedImages: Seq[String]): Repr
	
	
	// OTHER    -------------------------
	
	/**
	  * @param base64EncodedImage Image to include in this prompt. In Base 64 encoded string format.
	  * @return Copy of this item with the specified image attached
	  */
	def attachImage(base64EncodedImage: String) = attachImages(Single(base64EncodedImage))
}
