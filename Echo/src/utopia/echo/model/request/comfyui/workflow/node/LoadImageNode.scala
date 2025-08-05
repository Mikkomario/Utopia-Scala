package utopia.echo.model.request.comfyui.workflow.node

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.LoadImage
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.casting.ValueConversions._

/**
 * Used for loading images from the disk
 * @param name Name of this node
 * @param fileName Name of the image file to load. The file must reside in ComfyUI's input directory.
 *                 Note: If you're using the image as a latent image, make sure it's of correct size.
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
case class LoadImageNode(name: String, fileName: String) extends WorkflowNode
{
	override val classType: NodeClass = LoadImage
	
	override lazy val input: Model = Model.from("image" -> fileName)
	
	/**
	 * Reference to the loaded image
	 */
	lazy val image = output
	/**
	 * Reference to the loaded mask layer
	 */
	lazy val mask = outputAt(1)
}
