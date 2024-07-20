package utopia.echo.model.request

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.model.request.ApiRequest
import utopia.disciple.http.request.Body
import utopia.echo.model.LlmDesignator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.view.immutable.View

import scala.annotation.unused
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object Generate
{
	// COMPUTED -------------------------
	
	/**
	  * @param llm Name of the targeted LLM
	  * @return A factory for creating new generate requests
	  */
	def factory(implicit llm: LlmDesignator) = GenerateRequestFactory()
	
	
	// IMPLICIT -------------------------
	
	// Implicitly converts this object to a factory when the targeted LLM is known
	implicit def objectToFactory(@unused o: Generate.type)(implicit llm: LlmDesignator): GenerateRequestFactory =
		factory
	
	
	// NESTED   -------------------------
	
	/**
	  * Factory used for constructing various types of generate requests
	  * @param context Conversation context returned by the previous generate request.
	  *                Default = empty = new conversation.
	  * @param deprecationCondition A view which contains true if the request should be retracted,
	  *                             unless it has already been sent.
	  *                             None if the request should not be retracted under any circumstances (default).
	  * @param llm Name of the targeted LLM
	  */
	case class GenerateRequestFactory(context: Value = Value.empty, deprecationCondition: Option[View[Boolean]] = None)
	                            (implicit llm: LlmDesignator)
	{
		/**
		  * @param context New conversation context to apply
		  * @return Copy of this factory which uses the specified context instead of the current context
		  */
		def withContext(context: Value) = copy(context = context)
		
		/**
		  * @param condition A view which contains true if the request should be retracted,
		  *                  unless it has already been sent.
		  * @return Copy of this factory which uses the specified deprecation condition
		  *         instead of the currently specified condition.
		  */
		def withDeprecationCondition(condition: View[Boolean]) =
			copy(deprecationCondition = Some(condition))
		/**
		  * @param condition An (additional) deprecation condition to apply.
		  *                  Specified as a call-by-name value which yields true if the request should be retracted
		  *                  (unless it has already been sent).
		  * @return Copy of this factory which applies the specified deprecation condition.
		  *         If this factory already specified a deprecation condition,
		  *         either of these conditions may be met for the retraction to occur.
		  */
		def deprecatingIf(condition: => Boolean) = deprecationCondition match {
			case Some(existingCondition) => withDeprecationCondition(View { existingCondition.value || condition })
			case None => withDeprecationCondition(View(condition))
		}
		
		/**
		  * @param query Query to send out to the LLM
		  * @return A request for sending that query and for receiving the response as a single entity.
		  */
		def buffered(query: Query) = new GenerateBuffered(query, context, testDeprecation)
		/**
		  * @param prompt Prompt to present for the LLM
		  * @param context Additional contextual information to specify in the prompt or in the system message.
		  *                Default = empty.
		  * @param systemMessage A message that will override the LLM's system message from its Modelfile.
		  *                      Default = empty = use system message in the model's Modelfile.
		  * @param exc Implicit execution context utilized in streamed response-processing
		  * @param jsonParser Implicit json parser used in response-parsing
		  * @return A request for sending out the specified query and for receiving the response in a streamed format.
		  */
		def streamed(prompt: String, context: String = "", systemMessage: String = "")
		            (implicit exc: ExecutionContext, jsonParser: JsonParser) =
			new GenerateStreamed(prompt, this.context, context, systemMessage, testDeprecation)
		/**
		  * @param query Query to send out to the LLM
		  * @param stream Whether to receive the response as a stream.
		  *               - If false (default) the response will be received only once generation completes,
		  *               and will contain all the information at once.
		  *               - If true, the response text will be received word by word
		  * @param exc Implicit execution context utilized in streamed response-processing
		  * @param jsonParser Implicit json parser used in response-parsing
		  * @return A request for sending out the specified query
		  */
		def apply(query: Query, stream: Boolean = false)(implicit exc: ExecutionContext, jsonParser: JsonParser) =
			new GenerateBufferedOrStreamed(query, context, stream, testDeprecation)
			
		private def testDeprecation = deprecationCondition.forall { _.value }
	}
}

/**
  * Common trait for requests to the Ollama API generate endpoint.
  * These requests are used for sending a prompt and acquiring a reply.
  * This trait doesn't define whether the response is read in a streamed or in a buffered format.
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait Generate[R] extends ApiRequest[R]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Name of the targeted LLM
	  */
	def llm: LlmDesignator
	
	/**
	  * @return The query to send to the LLM
	  */
	def query: Query
	/**
	  * @return 'context' property returned by the last LLM response, if conversation context should be kept.
	  */
	def conversationContext: Value
	
	/**
	  * @return Whether to receive the response as a stream.
	  *         - If false the response will be received only once generation completes,
	  *         and will contain all the information at once.
	  *         - If true, the response text will be received word by word
	  */
	def stream: Boolean
	
	
	// IMPLEMENTED  ----------------------
	
	override def method: Method = Post
	override def path: String = "generate"
	
	override def body: Either[Value, Body] = Left(Model.from(
		"model" -> llm.name, "prompt" -> query.toPrompt,
		"format" -> (if (query.expectsJsonResponse) "json" else Value.empty),
		"system" -> query.toSystem, "context" -> conversationContext,
		"stream" -> stream))
}
