package utopia.echo.model.request.chat

import utopia.echo.model.{ChatMessage, LlmDesignator}
import utopia.echo.model.enumeration.ModelParameter
import utopia.echo.model.request.RequestParams
import utopia.echo.model.request.chat.tool.Tool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

/**
  * Parameters sent out with a chat request
  * @param message Message to send out
  * @param conversationHistory Conversation history displayed for the LLM (default = empty)
  * @param tools Tools made available to the LLM (default = empty)
  * @param options Behavioral settings (default = empty)
  * @param deprecationView A view which contains true if the request gets deprecated.
  *                        Only tacked until a request is sent.
  *                        Default = never deprecated.
  * @param llm Targeted LLM (implicit)
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
case class ChatParams(message: ChatMessage, conversationHistory: Seq[ChatMessage] = Empty, tools: Seq[Tool] = Empty,
                      options: Map[ModelParameter, Value] = Map(), deprecationView: View[Boolean] = AlwaysFalse)
                     (implicit override val llm: LlmDesignator)
	extends RequestParams[ChatParams]
{
	// COMPUTED ------------------------------
	
	/**
	 * @return All messages involved in this request. In chronological order.
	 */
	def messages = conversationHistory :+ message
	
	/**
	  * @return A request based on these parameters.
	  *         The streaming option must be specified before this request may be sent out.
	  */
	def toRequest = ChatRequest(this)
	
	
	// IMPLEMENTED  --------------------------
	
	override def toLlm(llm: LlmDesignator): ChatParams = copy()(llm = llm)
	override def withOptions(options: Map[ModelParameter, Value]): ChatParams = copy(options = options)
	override def withDeprecationView(condition: View[Boolean]): ChatParams = copy(deprecationView = condition)
	
	
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