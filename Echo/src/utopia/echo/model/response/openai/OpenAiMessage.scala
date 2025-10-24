package utopia.echo.model.response.openai

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead}
import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.openai.toolcall.OpenAiOutputElement
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.operator.Identity
import utopia.flow.util.StringExtensions._

import scala.util.Try

object OpenAiMessage extends OpenAiOutputElementFromModelFactory[OpenAiMessage]
{
	// ATTRIBUTES   -----------------------
	
	override lazy val typeIdentifiers: Set[String] = Set("message")
	private lazy val refusalType = "refusal"
	
	private lazy val schema = ModelDeclaration("id" -> IntType)
	
	private lazy val annotationParser = UrlCitation || OpenAiFileReference
	
	
	// IMPLEMENTED  -----------------------
	
	/**
	  * @param index Index of the message in the output sequence
	  * @return An interface for parsing a message from a model
	  */
	override def at(index: Int): OpenAiModelParser[OpenAiMessage] = new MessageAt(index)
	
	
	// NESTED   -------------------------
	
	private class MessageAt(index: Int) extends OpenAiModelParser[OpenAiMessage]
	{
		override def typeIdentifiers: Set[String] = OpenAiMessage.typeIdentifiers
		override def apply(model: HasProperties): Try[OpenAiMessage] = {
			// Makes sure the model contains the "id" property
			schema.validate(model).flatMap { model =>
				// Parses model content into either messages or refusals
				model("content").tryVectorWith { _.tryModel }.flatMap { contentModels =>
					val (messageModels, refusalModels) = contentModels
						.divideBy { _("type").getString == refusalType }.toTuple
					messageModels
						.tryMap { message =>
							// Parses message annotations & text
							message("annotations").tryVectorWith { _.tryModel.flatMap(annotationParser.apply) }
								.map { annotations =>
									val (urlCitations, fileReferences) = annotations.divideWith(Identity)
									(message("text").getString, urlCitations, fileReferences)
								}
						}
						.map { messages =>
							val text = messages.view.map { _._1 }.mkString
								.nonEmptyOrElse { refusalModels.view.map { _("refusal").getString }.mkString }
							val status: SchrodingerState = if (refusalModels.nonEmpty) Dead else parseStatusFrom(model)
							
							OpenAiMessage(index, model("id").getString, text,
								senderRole = model("role").string.flatMap(ChatRole.findForName).getOrElse(Assistant),
								urlCitations = messages.flatMap { _._2 }, fileReferences = messages.flatMap { _._3 },
								state = status, isRefusal = refusalModels.nonEmpty)
						}
				}
			}
		}
	}
}

/**
  * Represents an individual (chat) message, either from the user or the assistant / LLM.
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  * @param index Index at which this element appears in the output sequence
  * @param id Unique id of this message
  * @param text Text contents of this message
  * @param senderRole Role of this message's sender
  * @param urlCitations URL citations that apply to this message
  * @param fileReferences File references from this message
  * @param state State of this message (Alive = successful, Dead = Failed or refused, Flux = Processing)
  * @param isRefusal Whether this message represents a refusal
  */
case class OpenAiMessage(index: Int, id: String, text: String = "", senderRole: ChatRole = Assistant,
                         urlCitations: Seq[UrlCitation] = Empty, fileReferences: Seq[OpenAiFileReference] = Empty,
                         state: SchrodingerState = Alive, isRefusal: Boolean = false)
	extends OpenAiOutputElement
