package utopia.echo.model.request.comfyui.workflow.node

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.EmptyLatentImage
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model

/**
 * Used for creating empty latent image(s) for denoising
 * @param name Name of this node
 * @param size Width and height of the generated images, in pixels.
 *             Each should be a multiple of 64. Default = 512 x 512.
 * @param batchSize The number of images to generate (default = 1)
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
case class EmptyLatentImageNode(name: String, size: Pair[Int] = Pair.twice(512), batchSize: Int = 1)
	extends WorkflowNode
{
	override val classType: NodeClass = EmptyLatentImage
	
	override lazy val input: Model =
		Model.from("width" -> size.first, "height" -> size.second, "batch_size" -> batchSize)
	
	/**
	 * A reference to the latent image (batch)
	 */
	lazy val latent = output
}
