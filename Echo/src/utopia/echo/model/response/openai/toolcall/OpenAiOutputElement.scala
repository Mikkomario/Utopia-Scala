package utopia.echo.model.response.openai.toolcall

import utopia.annex.model.manifest.HasSchrodingerState

/**
  * Common trait for Open AI output elements, such as messages and tool calls.
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
trait OpenAiOutputElement extends HasSchrodingerState
{
	// ABSTRACT ------------------------
	
	/**
	  * @return 0-based index at which this element appears in the output sequence
	  */
	def index: Int
	/**
	  * @return ID of this element in the Open AI system
	  */
	def id: String
}
