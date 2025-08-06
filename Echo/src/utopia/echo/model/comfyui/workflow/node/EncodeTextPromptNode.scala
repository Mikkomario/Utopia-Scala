package utopia.echo.model.comfyui.workflow.node

import utopia.echo.model.comfyui.workflow.OutputRef
import utopia.echo.model.comfyui.workflow.node.NodeClass.EncodeTextPrompt
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.immutable.View

object EncodeTextPromptNode
{
	/**
	 * @param name Name of this node
	 * @param text The text to be encoded.
	 * @param clip A reference to the CLIP model used for encoding the text.
	 * @return A new node for encoding text (prompts)
	 */
	def apply(name: String, text: String, clip: OutputRef): EncodeTextPromptNode =
		apply(name, View.fixed(text), clip)
}

/**
 * Used for encoding input text and using it as a prompt
 *
 * @param name Name of this node
 * @param textView A view to the text to be encoded.
 * @param clip A reference to the CLIP model used for encoding the text.
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 */
case class EncodeTextPromptNode(name: String, textView: View[String], clip: OutputRef) extends WorkflowNode
{
	// ATTRIBUTES   ---------------------
	
	override val classType: NodeClass = EncodeTextPrompt
	
	private lazy val inputP = textView.mapValue { text => Model.from("text" -> text, "clip" -> clip) }
	
	/**
	 * A conditioning containing the embedded text used to guide the diffusion model.
	 */
	lazy val conditioning = output
	
	
	// IMPLEMENTED  ----------------------
	
	override def input: Model = inputP.value
}
