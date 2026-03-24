package utopia.echo.model.request

import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ReasoningEffort
import utopia.echo.model.enumeration.ReasoningEffort.SkipReasoning
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.ollama.RequestParams
import utopia.echo.model.request.ollama.chat.OllamaChatRequest
import utopia.echo.model.request.openai.GetBufferedOpenAiResponse
import utopia.echo.model.request.tool.Tool
import utopia.echo.model.settings.ModelSettings
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.language.implicitConversions
import scala.util.Try

object ChatParams
{
	// COMPUTED -------------------------
	
	/**
	 * @param defaultLlm LLM applied by default
	 * @return A model parser for chat parameters
	 */
	def parser(implicit defaultLlm: LlmDesignator): FromModelFactory[ChatParams] = new ChatParamsParser()
	
	
	// IMPLICIT -------------------------
	
	implicit def objectAsParser(o: ChatParams.type)(implicit defaultLlm: LlmDesignator): FromModelFactory[ChatParams] =
		o.parser
	
	
	// NESTED   -------------------------
	
	private class ChatParamsParser(implicit llm: LlmDesignator) extends FromModelFactory[ChatParams]
	{
		// IMPLEMENTED  -----------------
		
		// NB: Doesn't support tools at this time
		override def apply(model: HasProperties): Try[ChatParams] =
			model
				.tryGet("messages", "message") {
					_.tryVectorWith { _.tryModel.flatMap(ChatMessage.openAiMessageParser.apply) }.filter { _.nonEmpty }
				}
				.map { messages =>
					val settings = ModelSettings.parseFrom(model("options", "settings").model.getOrElse(model))
					val reasoningEffort = model("reasoning_effort").string match {
						case Some(effortStr) => ReasoningEffort.findForKey(effortStr)
						case None =>
							model("think").boolean.map { if (_) ReasoningEffort.Medium else SkipReasoning }
					}
					val appliedLlm = model("model").string.filterNot { _ ~== llm.llmName } match {
						case Some(modelName) => LlmDesignator(modelName)
						case None => llm
					}
					
					ChatParams(messages.head, messages.tail, settings = settings, reasoningEffort = reasoningEffort)(
						appliedLlm)
				}
	}
}

/**
  * Parameters sent out with a chat request
  * @param message Message to send out
  * @param conversationHistory Conversation history displayed for the LLM (default = empty)
  * @param tools Tools made available to the LLM (default = empty)
  * @param settings Behavioral settings (default = empty)
  * @param reasoningEffort Requested reasoning effort. None if the model's default should be used.
 * @param deprecationView A view which contains true if the request gets deprecated.
  *                        Only tacked until a request is sent.
  *                        Default = never deprecated.
 * @param llm Targeted LLM (implicit)
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
case class ChatParams(message: ChatMessage, conversationHistory: Seq[ChatMessage] = Empty, tools: Seq[Tool] = Empty,
                      settings: ModelSettings = ModelSettings.empty, reasoningEffort: Option[ReasoningEffort] = None,
                      deprecationView: View[Boolean] = AlwaysFalse)
                     (implicit override val llm: LlmDesignator)
	extends RequestParams[ChatParams]
{
	// COMPUTED ------------------------------
	
	/**
	 * @return All messages involved in this request. In chronological order.
	 */
	def messages = conversationHistory :+ message
	
	/**
	 * @return Whether this request has deprecated and should not be sent
	 */
	def deprecated = deprecationView.value
	
	/**
	  * @return A request based on these parameters.
	  *         The streaming option must be specified before this request may be sent out.
	  */
	def toRequest = OllamaChatRequest(this)
	/**
	 * @return A buffered Open AI request based on these parameters.
	 */
	def toOpenAiRequest = GetBufferedOpenAiResponse(this)
	
	
	// IMPLEMENTED  --------------------------
	
	override def toLlm(llm: LlmDesignator): ChatParams = copy()(llm = llm)
	override def withSettings(settings: ModelSettings): ChatParams = copy(settings = settings)
	override def withDeprecationView(condition: View[Boolean]): ChatParams = copy(deprecationView = condition)
	override def withReasoningEffort(effort: Option[ReasoningEffort]): ChatParams = copy(reasoningEffort = effort)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param history Conversation history to apply
	  * @return Copy of these parameters with the specified conversation history.
	  *         Note: Overwrites the current history.
	  */
	def withConversationHistory(history: Seq[ChatMessage]) = copy(conversationHistory = history)
	/**
	  * @param tools Tools made available to the LLM
	  * @return Copy of these parameters with the specified set of tools made available.
	  *         Note: Overwrites the current set of tools.
	  */
	def withTools(tools: Seq[Tool]) = copy(tools = tools)
	
	/**
	  * @param f A mapping function for modifying the wrapped message
	  * @return Copy of these parameters with a modified message
	  */
	def mapMessage(f: Mutate[ChatMessage]) = copy(message = f(message))
	/**
	  * @param f A mapping function for modifying the conversation history
	  * @return Copy of these parameters with a modified conversation history
	  */
	def mapConversationHistory(f: Mutate[Seq[ChatMessage]]) =
		withConversationHistory(f(conversationHistory))
	/**
	  * @param f A mapping function for modifying the list of provided tools
	  * @return Copy of these parameters with a modified list
	  */
	def mapTools(f: Mutate[Seq[Tool]]) = withTools(f(tools))
	
	/**
	  * @param tool Tool to include as an option for the LLM
	  * @return Copy of these parameters with the specified tool included
	  */
	def +(tool: Tool) = mapTools { _.appendIfDistinct(tool) }
	/**
	  * @param tools Tools to include as options for the LLM
	  * @return Copy of these parameters with the specified tools included
	  */
	def ++(tools: IterableOnce[Tool]) = mapTools { _.appendAllIfDistinct(tools) }
	
	/**
	  * @param historicalMessage A historical chat message
	  * @return Copy of these parameters with the specified message added at the beginning of the conversation history (
	  *         i.e. as the oldest message)
	  */
	def +:(historicalMessage: ChatMessage) = mapConversationHistory { historicalMessage +: _ }
	/**
	  * @param messageHistory Message history to prepend
	  * @return Copy of these parameters with the specified messages prepended to the conversation history.
	  *         I.e. these are placed as the oldest messages.
	  */
	def ++:(messageHistory: Seq[ChatMessage]) = mapConversationHistory { messageHistory ++ _ }
}