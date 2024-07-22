package utopia.echo.model.response.chat

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Final, PositiveFlux}
import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.ResponseStatistics
import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object StreamedReplyMessage
{
	def empty(lastUpdated: Instant = Now, role: ChatRole = Assistant)(implicit exc: ExecutionContext) =
		completed(Success(ResponseStatistics.empty), lastUpdated = lastUpdated, role = role)
	
	def success(text: String, statistics: ResponseStatistics, lastUpdated: Instant = Now, role: ChatRole = Assistant)
	           (implicit exc: ExecutionContext) =
		completed(Success(statistics), text, lastUpdated, role)
		
	def failure(cause: Throwable)(implicit exc: ExecutionContext) = completed(Failure(cause))
	
	def completed(statistics: Try[ResponseStatistics], text: String = "", lastUpdated: Instant = Now,
	              role: ChatRole = Assistant)
	             (implicit exc: ExecutionContext) =
		apply(Fixed(text), Fixed(lastUpdated), Future.successful(statistics), role)
	
	def apply(textPointer: Changing[String], lastUpdatedPointer: Changing[Instant],
	          statisticsFuture: Future[Try[ResponseStatistics]], role: ChatRole = Assistant)
	         (implicit exc: ExecutionContext) =
		new StreamedReplyMessage(textPointer, lastUpdatedPointer, statisticsFuture, role)
}

/**
  * Represents a chat reply received from an LLM. Incrementally built based on the streamed data.
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
class StreamedReplyMessage(override val textPointer: Changing[String],
                           override val lastUpdatedPointer: Changing[Instant],
                           override val statisticsFuture: Future[Try[ResponseStatistics]],
                           override val senderRole: ChatRole = Assistant)
                          (implicit exc: ExecutionContext)
	extends ReplyMessage
{
	// ATTRIBUTES   -----------------------
	
	override lazy val future = statisticsFuture.mapIfSuccess { stats =>
		BufferedReplyMessage(ChatMessage(text, senderRole), stats)
	}
	
	
	// COMPUTED ---------------------------
	
	override def text = textPointer.value
	override def lastUpdated = lastUpdatedPointer.value
	
	override def state: SchrodingerState = future.currentResult match {
		case Some(result) => Final(result.flatten.isSuccess)
		case None => PositiveFlux
	}
}
