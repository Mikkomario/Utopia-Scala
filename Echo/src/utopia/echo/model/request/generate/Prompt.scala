package utopia.echo.model.request.generate

import utopia.echo.model.request.CanAttachImages
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.Mutate
import utopia.flow.util.UncertainNumber.UncertainInt

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
	// COMPUTED ---------------------------
	
	/**
	  * @return This prompt as a query
	  */
	def toQuery = Query(this)
	
	
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
	
	/**
	 * @param f A mapping function applied to this prompt's context parameter
	 * @return Copy of this prompt with a modified context
	 */
	def mapContext(f: Mutate[String]) = withContext(f(context))
	/**
	 * @param f A mapping function applied to this prompt's system message
	 * @return Copy of this prompt with a modified system message
	 */
	def mapSystemMessage(f: Mutate[String]) = withSystemMessage(f(systemMessage))
	
	/**
	  * @param numberOfExpectedResponses Number of responses to expect from the LLM
	  * @return A query with this prompt, plus a prompt to respond as a json array with that many responses
	  */
	def toMultiQuery(numberOfExpectedResponses: UncertainInt) =
		Query(this, numberOfExpectedResponses = numberOfExpectedResponses, requestJson = true)
	/**
	  * @param schema Schema which describes how the LLM should respond
	  * @param numberOfExpectedResponses Number of individual response values expected in the model response.
	  *                                  Default = exactly 1.
	  * @return A query with this prompt and the specified schema
	  */
	def toQueryWithSchema(schema: ObjectSchema, numberOfExpectedResponses: UncertainInt = 1) =
		Query(this, schema, numberOfExpectedResponses, requestJson = true)
}