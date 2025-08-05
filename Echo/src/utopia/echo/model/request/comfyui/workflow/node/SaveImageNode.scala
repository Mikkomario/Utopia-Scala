package utopia.echo.model.request.comfyui.workflow.node

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.SaveImage
import utopia.echo.model.request.comfyui.workflow.OutputRef
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.casting.ValueConversions._

/**
 * A node which saves an image to the ComfyUI output directory
 * @param name Name of this node
 * @param image Reference to the image to save
 * @param fileNamePrefix Prefix for the file to save. Default = "ComfyUI".
 *
 *                       This may include formatting information such as %date:yyyy-MM-dd% or
 *                       %Empty Latent Image.width% to include values from nodes.
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
case class SaveImageNode(name: String, image: OutputRef, fileNamePrefix: String = "ComfyUI")
	extends WorkflowNode
{
	override val classType: NodeClass = SaveImage
	
	override lazy val input: Model = Model.from("images" -> image, "filename_prefix" -> fileNamePrefix)
}
