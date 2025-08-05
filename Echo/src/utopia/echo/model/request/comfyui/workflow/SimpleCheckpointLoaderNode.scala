package utopia.echo.model.request.comfyui.workflow

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.SimpleCheckpointLoader
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.casting.ValueConversions._

/**
 * A node used for loading a checkpoint model
 *
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 * @param name Name of this node
 * @param modelName The name of the checkpoint (model) to load.
 */
case class SimpleCheckpointLoaderNode(name: String, modelName: String) extends WorkflowNode
{
	// ATTRIBUTES   --------------------
	
	override val classType: NodeClass = SimpleCheckpointLoader
	
	override lazy val input: Model = Model.from("ckpt_name" -> modelName)
	
	/**
	 * @return Reference to the model used for denoising latents.
	 */
	lazy val model = output
	/**
	 * @return Reference to the CLIP model used for encoding text prompts.
	 */
	lazy val clip = outputAt(1)
	/**
	 * @return Reference to the VAE model used for encoding and decoding images to and from latent space.
	 */
	lazy val vae = outputAt(2)
}
