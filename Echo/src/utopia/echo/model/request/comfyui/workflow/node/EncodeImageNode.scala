package utopia.echo.model.request.comfyui.workflow.node

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.EncodeImage
import utopia.echo.model.request.comfyui.workflow.OutputRef
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model

/**
 * Used for encoding an image into a VAE, so that it may be used as the latent image, etc.
 *
 * @param name Name of this node
 * @param image Reference to the image to encode
 * @param vae Reference to the VAE encoding model
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
case class EncodeImageNode(name: String, image: OutputRef, vae: OutputRef) extends WorkflowNode
{
	override val classType: NodeClass = EncodeImage
	
	override lazy val input: Model = Model.from("pixels" -> image, "vae" -> vae)
	
	/**
	 * Reference to the encoded latent image
	 */
	lazy val latent = output
}
