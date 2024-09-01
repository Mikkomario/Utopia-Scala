package utopia.echo.model.request.chat

import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.User
import utopia.echo.model.request.RetractableRequestFactory
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.View

@deprecated("Deprecated for removal", "v1.1")
object ChatRequestFactory
{
	// OTHER    --------------------------
	
	/**
	  * @param f A function for constructing chat message requests.
	  *          Accepts:
	  *             1. Message to send
	  *             1. Conversation history
	  *             1. Deprecation condition
	  *
	  *          And yields a chat request
	  * @tparam A Type of constructed requests
	  * @return A factory used for constructing chat requests
	  */
	def apply[A](f: (ChatMessage, Seq[ChatMessage], () => Boolean) => A): ChatRequestFactory[A] =
		new _ChatRequestFactory[A](Empty, None)(f)
	
	
	// NESTED   --------------------------
	
	private class _ChatRequestFactory[+A](conversationHistory: Seq[ChatMessage],
	                                      override val deprecationCondition: Option[View[Boolean]])
	                                     (f: (ChatMessage, Seq[ChatMessage], () => Boolean) => A)
		extends ChatRequestFactory[A]
	{
		// ATTRIBUTES   ------------------
		
		private lazy val testDeprecation = deprecationCondition match {
			case Some(condition) => () => condition.value
			case None => () => false
		}
		
		
		// IMPLEMENTED  ------------------
		
		override def withConversationHistory(previousMessages: Seq[ChatMessage]) =
			new _ChatRequestFactory(previousMessages, deprecationCondition)(f)
		
		override def withDeprecationCondition(condition: View[Boolean]) =
			new _ChatRequestFactory(conversationHistory, Some(condition))(f)
		
		override def apply(message: ChatMessage): A = f(message, conversationHistory, testDeprecation)
	}
}

/**
  * Common trait for factories used for creating chat requests
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
@deprecated("Deprecated for removal", "v1.1")
trait ChatRequestFactory[+A] extends RetractableRequestFactory[ChatRequestFactory[A]]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param previousMessages Previous messages to include as request context
	  * @return Copy of this factory with the specified message history
	  */
	def withConversationHistory(previousMessages: Seq[ChatMessage]): ChatRequestFactory[A]
	
	/**
	  * @param message Message to send to the LLM
	  * @return A request for sending the specified message
	  */
	def apply(message: ChatMessage): A
	
	
	// OTHER    --------------------------
	
	/**
	  * @param messageText Message text to send to the LLM
	  * @return A request for sending the specified message
	  */
	def apply(messageText: String): A = apply(ChatMessage(messageText, User))
}
