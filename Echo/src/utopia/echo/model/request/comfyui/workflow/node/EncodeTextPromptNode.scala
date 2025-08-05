package utopia.echo.model.request.comfyui.workflow.node

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.EncodeTextPrompt
import utopia.echo.model.request.comfyui.workflow.OutputRef
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model

/**
 * Used for encoding input text and using it as a prompt
 *
 * @param name Name of this node
 * @param text The text to be encoded.
 * @param clip A reference to the CLIP model used for encoding the text.
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 */
case class EncodeTextPromptNode(name: String, text: String, clip: OutputRef) extends WorkflowNode
{
	// ATTRIBUTES   ---------------------
	
	override val classType: NodeClass = EncodeTextPrompt
	
	override lazy val input: Model = Model.from("text" -> text, "clip" -> clip)
	
	/**
	 * A conditioning containing the embedded text used to guide the diffusion model.
	 */
	lazy val conditioning = output
}
