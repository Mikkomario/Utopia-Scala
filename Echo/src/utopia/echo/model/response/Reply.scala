package utopia.echo.model.response

import utopia.annex.model.manifest.SchrodingerState
import utopia.echo.model.ChatMessage
import utopia.flow.async.AsyncExtensions._
import utopia.flow.view.template.eventful.{Changing, Flag}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Reply
{
	// OTHER    -----------------------------
	
	/**
	 * Prepares a new streaming reply instance
	 * @param textPointer A pointer that contains this reply's full text
	 * @param thoughtsPointer A pointer that contains this reply's reflective / thinking content
	 * @param newTextPointer A pointer that contains the latest text increment
	 * @param thinkingFlag A flag that contains true while the thinking content may still be generated
	 * @param lastUpdatedPointer A pointer that contains a timestamp of this reply's latest update
	 * @return A new streaming reply
	 */
	def streaming(textPointer: Changing[String], thoughtsPointer: Changing[String], newTextPointer: Changing[String],
	              thinkingFlag: Flag, lastUpdatedPointer: Changing[Instant]) =
		new StreamingReplyFactory(textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer)
	
	
	// NESTED   -----------------------------
	
	class StreamingReplyFactory(textPointer: Changing[String], thoughtsPointer: Changing[String],
	                            newTextPointer: Changing[String], thinkingFlag: Flag,
	                            lastUpdatedPointer: Changing[Instant])
	{
		/**
		 * @param future A future that resolves into a buffered version of this reply
		 * @return A streaming reply that resolves based on the specified future
		 */
		def futureBuffered(future: Future[Try[BufferedReply]]): Reply =
			new ReplyWrapper(ReplyLike.streaming(textPointer, thoughtsPointer, newTextPointer, thinkingFlag,
				lastUpdatedPointer, future))
		/**
		 * @param future A future that resolves into token-usage statistics on request completion
		 * @param exc Implicit execution context
		 * @return A streaming reply that completes once the specified future resolves
		 */
		def futureStatistics(future: Future[Try[TokenUsage]])(implicit exc: ExecutionContext): Reply =
			futureBuffered(future.mapSuccess { usage =>
				BufferedReply(ChatMessage(textPointer.value, thoughtsPointer.value), usage)
			})
	}
	
	private class ReplyWrapper(wrapped: ReplyLike[BufferedReply]) extends Reply
	{
		override def isBuffered: Boolean = wrapped.isBuffered
		override def future: Future[Try[BufferedReply]] = wrapped.future
		
		override def text: String = wrapped.text
		override def textPointer: Changing[String] = wrapped.textPointer
		
		override def thoughts: String = wrapped.thoughts
		override def thoughtsPointer: Changing[String] = wrapped.thoughtsPointer
		
		override def newTextPointer: Changing[String] = wrapped.newTextPointer
		override def thinkingFlag: Flag = wrapped.thinkingFlag
		
		override def lastUpdated: Instant = wrapped.lastUpdated
		override def lastUpdatedPointer: Changing[Instant] = wrapped.lastUpdatedPointer
		
		override def state: SchrodingerState = wrapped.state
	}
}

/**
 * Common trait / interface for LLM replies, whether they're streamed or buffered.
 * @author Mikko Hilpinen
 * @since 03.09.2025, v1.4
 */
trait Reply extends ReplyLike[BufferedReply]
