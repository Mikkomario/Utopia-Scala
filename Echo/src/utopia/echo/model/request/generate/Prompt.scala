package utopia.echo.model.request.generate

import utopia.echo.model.request.CanAttachImages
import utopia.flow.collection.immutable.Empty

import scala.language.implicitConversions

object Prompt
{
	// Implicitly converts strings to prompts
	implicit def textToPrompt(text: String): Prompt = apply(text)
}

/**
  * Represents a prompt to an LLM. Possibly includes contextual information as well.
  * @param text The primary prompt (text)
  * @param context Contextual information to include in the prompt or the system message.
  *                Default = empty = no additional context is given.
  * @param systemMessage A message that will override the LLM's system message from its Modelfile.
  *                      Default = empty = use system message in the model's Modelfile.
  * @param encodedImages Images to sent to the LLMm, as Base64 encoded strings. Default = empty.
  *                      Please note that only certain LLMs support image input.
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class Prompt(text: String, context: String = "", systemMessage: String = "", encodedImages: Seq[String] = Empty)
	extends CanAttachImages[Prompt]
{
	// IMPLEMENTED  -----------------------
	
	override def attachImages(base64EncodedImages: Seq[String]): Prompt =
		copy(encodedImages = encodedImages ++ base64EncodedImages)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param context New context to assign
	  * @return Copy of this prompt with the specified context
	  */
	def withContext(context: String) = copy(context = context)
	/**
	  * @param systemMessage New system message to assign.
	  *                      Will overwrite whatever system message is defined in the LLM's Modelfile.
	  * @return Copy of this prompt with the specified system message
	  */
	def withSystemMessage(systemMessage: String) = copy(systemMessage = systemMessage)
}