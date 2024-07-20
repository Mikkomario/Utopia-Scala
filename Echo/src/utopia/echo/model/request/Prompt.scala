package utopia.echo.model.request

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
{
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
	
	/**
	  * @param base64EncodedImages Images to include in this prompt (will overwrite existing images).
	  *                            Specify all images as Base 64 encoded strings.
	  * @return Copy of this prompt with the specified images
	  */
	def withImages(base64EncodedImages: Seq[String]) = copy(encodedImages = base64EncodedImages)
	/**
	  * @param base64EncodedImage Image to include in this prompt. In Base 64 encoded string format.
	  * @return Copy of this prompt with the specified image included
	  */
	def withImageAdded(base64EncodedImage: String) = withImages(encodedImages :+ base64EncodedImage)
}