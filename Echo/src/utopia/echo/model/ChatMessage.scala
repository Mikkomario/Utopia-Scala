package utopia.echo.model

import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.{Assistant, User}
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.{CanAttachImages, ChatParams}
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.util.{Mutate, NotEmpty}

import scala.util.Try

object ChatMessage
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * An empty chat message from the user
	 */
	lazy val empty = apply("")
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return A model parser used with responses originating from Ollama
	 */
	def ollamaMessageParser: FromModelFactory[ChatMessage] = OllamaMessageParser
	/**
	 * @return A model parser used with responses originating from DeepSeek or OpenAI's /chat/completions endpoint.
	 */
	def deepSeekMessageParser: FromModelFactory[ChatMessage] = DeepSeekMessageParser
	
	
	// NESTED   -----------------------
	
	private object OllamaMessageParser extends FromModelFactory[ChatMessage]
	{
		override def apply(model: HasProperties): Try[ChatMessage] =
			model("tool_calls").tryVectorWith { _.tryModel.flatMap(ToolCall.apply) }.map { toolCalls =>
				ChatMessage(model("content").getString, model("thinking").getString,
					model("role").string.flatMap(ChatRole.findForName).getOrElse(Assistant),
					model("images").getVector.flatMap { _.string }, toolCalls)
			}
	}
	
	private object DeepSeekMessageParser extends FromModelFactory[ChatMessage]
	{
		override def apply(model: HasProperties): Try[ChatMessage] = {
			model("tool_calls").tryVectorWith { _.tryModel.flatMap(ToolCall.apply) }.map { toolCalls =>
				ChatMessage(model("content").getString, model("reasoning_content").getString,
					model("role").string.flatMap(ChatRole.findForName).getOrElse(Assistant), toolCalls = toolCalls)
			}
		}
	}
}

/**
  * Represents a message sent or received via the Ollama chat interface
  * @param text Text within this message
  * @param senderRole Role of the entity that sent this message (default = user)
  * @param encodedImages Base 64 encoded images attached to this message.
  *                     Empty if no image is attached (default).
  * @param toolCalls Tool calls made by this message
 * @param respondedToolCallId ID of the tool call that was responded to.
 *                   Specify this only when sender role = Tool.
 * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class ChatMessage(text: String, thoughts: String = "", senderRole: ChatRole = User,
                       encodedImages: Seq[String] = Empty, toolCalls: Seq[ToolCall] = Empty,
                       respondedToolCallId: String = "")
	extends ModelConvertible with CanAttachImages[ChatMessage]
{
	// COMPUTED ----------------------------
	
	/**
	 * @return A copy of this message where the sender role is set to [[Assistant]]
	 */
	def fromAssistant = withSenderRole(Assistant)
	
	/**
	  * @param llm Targeted LLM
	  * @return Request parameters for sending out this message
	  */
	def toRequestParams(implicit llm: LlmDesignator) = ChatParams(this)
	
	
	// IMPLEMENTED  ------------------------
	
	// NB: Thoughts are not included in this model (not supported as LLM input)
	// TODO: Divide into request model and regular model that contains all information
	override def toModel: Model =
		Model.from("role" -> senderRole.name, "content" -> text.replace("\t", "  "),
				"images" -> NotEmpty(encodedImages), "tool_calls" -> NotEmpty(toolCalls),
				"tool_call_id" -> respondedToolCallId)
			.withoutEmptyValues
	
	override def attachImages(base64EncodedImages: Seq[String]): ChatMessage =
		copy(encodedImages = encodedImages ++ base64EncodedImages)
	
	override def toString = {
		val thoughtsStr = thoughts.mapIfNotEmpty { t => s"(Thinking: $t) " }
		val toolsStr = NotEmpty(toolCalls) match {
			case Some(calls) => s" calling ${ calls.mkString(" & ") }"
			case None => ""
		}
		val imagesStr = if (encodedImages.nonEmpty) s" with ${ encodedImages.size } images" else ""
		s"$senderRole: $thoughtsStr$text$imagesStr$toolsStr"
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param role Role of this message's new sender
	 * @return Copy of this message with the specified sender role
	 */
	def withSenderRole(role: ChatRole) = if (senderRole == role) this else copy(senderRole = role)
	
	/**
	  * @param message Reply message
	  * @return A message from the recipient of this message, with the specified text
	  */
	def replyWith(message: String) = ChatMessage(message, senderRole = senderRole.opposite)
	
	/**
	  * @param moreText More text to add to this message
	  * @return A copy of this message with appended text
	  */
	def +(moreText: String) = mapText { _ + moreText }
	
	/**
	  * @param f A mapping function applied to this message's text
	  * @return A modified copy of this message
	  */
	def mapText(f: Mutate[String]) = copy(text = f(text))
}