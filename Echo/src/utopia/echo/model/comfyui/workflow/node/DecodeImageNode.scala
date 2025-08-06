package utopia.echo.model.comfyui.workflow.node

import utopia.echo.model.comfyui.workflow.node.NodeClass
import utopia.echo.model.comfyui.workflow.node.NodeClass.DecodeImage
import utopia.echo.model.comfyui.workflow.OutputRef
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model

/**
 * A node which decodes latent images back into pixel space images.
 *
 * @param name Name of this node
 * @param latent The encoded latent image
 * @param vae The VAE model for decoding the latent
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
case class DecodeImageNode(name: String, latent: OutputRef, vae: OutputRef) extends WorkflowNode
{
	override val classType: NodeClass = DecodeImage
	
	override lazy val input: Model = Model.from("samples" -> latent, "vae" -> vae)
	
	/**
	 * A reference to the encoded image
	 */
	lazy val image = output
}