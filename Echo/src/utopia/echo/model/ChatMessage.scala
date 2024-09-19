package utopia.echo.model

import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.User
import utopia.echo.model.request.CanAttachImages
import utopia.echo.model.request.chat.ChatParams
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.util.NotEmpty

import scala.util.Try

object ChatMessage extends FromModelFactory[ChatMessage]
{
	// IMPLEMENTED  ----------------------
	
	override def apply(model: ModelLike[Property]): Try[ChatMessage] =
		model("role").tryString.flatMap(ChatRole.forName).flatMap { role =>
			model("tool_calls").getVector.tryMap { v => ToolCall(v.getModel) }.map { toolCalls =>
				apply(
					model("content", "text").getString, role,
					model("images").getVector.flatMap { _.string }, toolCalls)
			}
		}
		
	
	// OTHER    -------------------------
	
	/**
	  * Parses a chat message from a model
	  * @param model Model to parse this message from
	  * @param defaultRole Role of this message's sender, if not specified in the model
	  * @return A chat message parsed from the specified model
	  */
	def parseFrom(model: AnyModel, defaultRole: => ChatRole) =
		apply(model("content", "text").getString,
			model("role").string.flatMap(ChatRole.findForName).getOrElse(defaultRole),
			model("images").getVector.flatMap { _.string },
			model("tool_calls").getVector.flatMap { v => v.model.flatMap { ToolCall(_).toOption } })
}

/**
  * Represents a message sent or received via the Ollama chat interface
  * @param text Text within this message
  * @param senderRole Role of the entity that sent this message (default = user)
  * @param encodedImages Base 64 encoded images attached to this message.
  *                     Empty if no image is attached (default).
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class ChatMessage(text: String, senderRole: ChatRole = User, encodedImages: Seq[String] = Empty,
                       toolCalls: Seq[ToolCall] = Empty)
	extends ModelConvertible with CanAttachImages[ChatMessage]
{
	// COMPUTED ----------------------------
	
	/**
	  * @param llm Targeted LLM
	  * @return Request parameters for sending out this message
	  */
	def toRequestParams(implicit llm: LlmDesignator) = ChatParams(this)
	
	
	// IMPLEMENTED  ------------------------
	
	override def toModel: Model =
		Model.from("role" -> senderRole.name, "content" -> text,
				"images" -> NotEmpty(encodedImages), "tool_calls" -> NotEmpty(toolCalls))
			.withoutEmptyValues
	
	override def attachImages(base64EncodedImages: Seq[String]): ChatMessage =
		copy(encodedImages = encodedImages ++ base64EncodedImages)
	
	override def toString = {
		val toolsStr = NotEmpty(toolCalls) match {
			case Some(calls) => s" calling ${ calls.mkString(" & ") }"
			case None => ""
		}
		val imagesStr = if (encodedImages.nonEmpty) s" with ${ encodedImages.size } images" else ""
		s"$senderRole: $text$imagesStr$toolsStr"
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param message Reply message
	  * @return A message from the recipient of this message, with the specified text
	  */
	def replyWith(message: String) = ChatMessage(message, senderRole.opposite)
}