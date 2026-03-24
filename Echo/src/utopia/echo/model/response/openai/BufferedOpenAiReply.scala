package utopia.echo.model.response.openai

import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.MessageStopReason
import utopia.echo.model.enumeration.MessageStopReason._
import utopia.echo.model.response.{BufferedReply, BufferedReplyLike}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{ModelType, StringType}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now

import java.time.Instant
import scala.util.Try

object BufferedOpenAiReply extends FromModelFactory[BufferedOpenAiReply]
{
	// ATTRIBUTES   -------------------------
	
	private lazy val schema = ModelDeclaration("id" -> StringType, "usage" -> ModelType)
	
	/**
	 * An empty reply, that may be used as a placeholder
	 */
	lazy val empty = apply("", Empty, OpenAiTokenUsageStatistics.zero)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(model: HasProperties): Try[BufferedOpenAiReply] = {
		schema.validate(model).flatMap { model =>
			// Parses the "choices" (messages)
			model("choices").tryVector
				.flatMap { values =>
					values.zipWithIndex.tryMapAll { case (value, index) =>
						value.tryModel.flatMap { choiceModel =>
							choiceModel.tryGet("message") { _.tryModel.flatMap(ChatMessage.openAiMessageParser.apply) }
								.map { (_, choiceModel, choiceModel("index").intOr(index)) }
						}
					}
				}
				.flatMap { choices =>
					// Parses the token usage
					model.tryGet("usage") {
							_.tryModel.flatMap(OpenAiTokenUsageStatistics.chatCompletionParser.apply) }
						.map { tokenUsage =>
							val orderedChoices = choices.sortBy { _._3 }
							// Determines the last stop choice
							val stopChoice = orderedChoices.reverseIterator.findMap { case (_, model, _) =>
								model("finish_reason").string.flatMap {
									case "stop" => Some(MessageCompleted)
									case "length" => Some(LengthLimitReached)
									case "content_filter" => Some(Censored)
									case "tool_calls" | "function_call" => Some(ToolCalled)
									case "insufficient_system_resource" => Some(ServerError)
									case _ => None
								}
							}
							apply(model("id").getString, orderedChoices.map { _._1 }, tokenUsage,
								stopChoice.getOrElse(MessageCompleted),
								model("created").long
									.flatMap { unixSeconds => Try { Instant.ofEpochSecond(unixSeconds) }.toOption }
									.getOrElse(Now))
						}
				}
		}
	}
}

/**
 * Represents a buffered response acquired from a chat/completions API request
 * @param id ID of this reply
 * @param messages Returned chat messages
 * @param tokenUsage Statistics about the token usage
 * @param stopReason Reason why generation was stopped (default = message completed)
 * @param lastUpdated Time when this reply was created / finished (default = now)
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.5
 */
case class BufferedOpenAiReply(id: String, messages: Seq[ChatMessage], tokenUsage: OpenAiTokenUsageStatistics,
                               stopReason: MessageStopReason = MessageCompleted, lastUpdated: Instant = Now)
	extends BufferedReply with BufferedReplyLike[BufferedOpenAiReply] with ModelConvertible
{
	// ATTRIBUTES   ------------------------
	
	override lazy val text: String = messages.iterator.map { _.text }.filter { _.nonEmpty }.mkString("\n")
	override lazy val thoughts: String = messages.iterator.map { _.thoughts }.filter { _.nonEmpty }.mkString("\n")
	
	
	// IMPLEMENTED  ------------------------
	
	override def self: BufferedOpenAiReply = this
	
	override def message: ChatMessage = messages.lastOption.getOrElse(ChatMessage.empty.fromAssistant)
	
	override def toModel: Model = Model.from("id" -> id, "messages" -> messages.map { _.toModelIncludingThoughts },
		"usage" -> tokenUsage, "finish_reason" -> stopReason.key, "created" -> lastUpdated)
}