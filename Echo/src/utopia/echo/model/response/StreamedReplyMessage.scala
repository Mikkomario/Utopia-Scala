package utopia.echo.model.response

import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.flow.async.AsyncExtensions._
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object StreamedReplyMessage
{
	// TODO: Add constructors
}

/**
  * Represents a chat reply received from an LLM. Incrementally built based on the streamed data.
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
class StreamedReplyMessage(val textPointer: Changing[String], val lastUpdatedPointer: Changing[Instant],
                           val statisticsFuture: Future[Try[ResponseStatistics]],
                           val role: ChatRole = Assistant)(implicit exc: ExecutionContext)
{
	// ATTRIBUTES   -----------------------
	
	lazy val future = statisticsFuture.mapIfSuccess { stats => BufferedReplyMessage(ChatMessage(text, role), stats) }
	
	
	// COMPUTED ---------------------------
	
	def text = textPointer.value
	
	def lastUpdated = lastUpdatedPointer.value
}
