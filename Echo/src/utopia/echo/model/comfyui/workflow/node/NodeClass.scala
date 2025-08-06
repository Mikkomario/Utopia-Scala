package utopia.echo.model.comfyui.workflow.node

import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.util.{OpenEnumeration, OpenEnumerationValue}

/**
 * An open enumeration for different ComfyUI workflow node classes (i.e. possible values of "class_type").
 *
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 */
trait NodeClass extends OpenEnumerationValue[String]

object NodeClass extends OpenEnumeration[NodeClass, String](identifiersMatch = EqualsFunction.stringCaseInsensitive)
{
	// INITIAL CODE ----------------------
	
	introduce(SimpleCheckpointLoader, EncodeTextPrompt, KSampler,
		EncodeImage, DecodeImage, EmptyLatentImage, SaveImage, LoadImage)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param classType A class type name
	 * @return Node class matching that name
	 */
	def apply(classType: String): NodeClass = findFor(classType).getOrElse { new _NodeClass(classType) }
	
	
	// VALUES   --------------------------
	
	/**
	 * Loads a diffusion model checkpoint, diffusion models are used to denoise latents.
	 */
	case object SimpleCheckpointLoader extends NodeClass
	{
		override lazy val identifier: String = "CheckpointLoaderSimple"
	}
	
	/**
	 * Encodes a text prompt using a CLIP model into an embedding
	 * that can be used to guide the diffusion model towards generating specific images.
	 */
	case object EncodeTextPrompt extends NodeClass
	{
		override lazy val identifier: String = "CLIPTextEncode"
	}
	
	/**
	 * Uses the provided model, positive and negative conditioning to denoise the latent image.
	 */
	case object KSampler extends NodeClass
	{
		override lazy val identifier: String = "KSampler"
	}
	
	/**
	 * Encodes an image from pixels using a model's VAE
	 */
	case object EncodeImage extends NodeClass
	{
		override lazy val identifier: String = "VAEEncode"
	}
	/**
	 * Decodes latent images back into pixel space images.
	 */
	case object DecodeImage extends NodeClass
	{
		override lazy val identifier: String = "VAEDecode"
	}
	
	/**
	 * Create a new batch of empty latent images to be denoised via sampling.
	 */
	case object EmptyLatentImage extends NodeClass
	{
		override lazy val identifier: String = "EmptyLatentImage"
	}
	/**
	 * Saves the input images to your ComfyUI output directory.
	 */
	case object SaveImage extends NodeClass
	{
		override lazy val identifier: String = "SaveImage"
	}
	/**
	 * Loads an image from the disk
	 */
	case object LoadImage extends NodeClass
	{
		override lazy val identifier: String = "LoadImage"
	}
	
	
	// NESTED   --------------------------
	
	private class _NodeClass(override val identifier: String) extends NodeClass
	{
		introduce(this)
	}
}
