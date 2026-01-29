package utopia.echo.model.response

import utopia.annex.model.manifest.{HasSchrodingerState, SchrodingerState}
import utopia.flow.operator.Identity
import utopia.flow.util.Mutate
import utopia.flow.view.template.eventful.{Changing, Flag}

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

object ReplyLike
{
	// OTHER    -----------------------------
	
	/**
	 * Prepares a new streaming reply instance
	 * @param textPointer A pointer that contains this reply's full text
	 * @param thoughtsPointer A pointer that contains this reply's reflective / thinking content
	 * @param newTextPointer A pointer that contains the latest text increment
	 * @param thinkingFlag A flag that contains true while the thinking content may still be generated
	 * @param lastUpdatedPointer A pointer that contains a timestamp of this reply's latest update
	 * @param future A future that resolves into a buffered version of this reply
	 * @return A new streaming reply
	 */
	def streaming[B](textPointer: Changing[String], thoughtsPointer: Changing[String], newTextPointer: Changing[String],
	              thinkingFlag: Flag, lastUpdatedPointer: Changing[Instant], future: Future[Try[B]]): ReplyLike[B] =
		new StreamingReply(textPointer, thoughtsPointer, newTextPointer, thinkingFlag, lastUpdatedPointer, future)
	
	
	// NESTED   -----------------------------
	
	private class StreamingReply[+B](override val textPointer: Changing[String],
	                                 override val thoughtsPointer: Changing[String],
	                                 override val newTextPointer: Changing[String],
	                                 override val thinkingFlag: Flag,
	                                 override val lastUpdatedPointer: Changing[Instant],
	                                 override val future: Future[Try[B]])
		extends ReplyLike[B]
	{
		override def text: String = textPointer.value
		override def thoughts: String = thoughtsPointer.value
		override def lastUpdated: Instant = lastUpdatedPointer.value
		
		override def isBuffered: Boolean = future.isCompleted
		override def state: SchrodingerState = SchrodingerState.of(future)
	}
}

/**
  * Common trait / interface for LLM replies, whether they're streamed or buffered.
  * @tparam Buffered Buffered version of this response
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait ReplyLike[+Buffered] extends HasSchrodingerState
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Whether this response has been fully read / buffered already
	  */
	def isBuffered: Boolean
	
	/**
	  * @return A future that resolves into a buffered / completed version of this response, once read.
	  *         Will yield a failure in case response-parsing failed.
	  */
	def future: Future[Try[Buffered]]
	
	/**
	  * @return The current text in this response.
	  *         Note: Might not be final.
	  */
	def text: String
	/**
	  * @return A pointer which contains the currently built response text
	  */
	def textPointer: Changing[String]
	
	/**
	 * @return The reflective / reasoning content produced by the LLM before the final answer.
	 *         May be empty.
	 */
	def thoughts: String
	/**
	 * @return A pointer that contains the reflective / reasoning content produced by the LLM
	 *         before giving the final answer.
	 */
	def thoughtsPointer: Changing[String]
	
	/**
	 * @return A pointer which contains the latest read reply message addition.
	 *         Note: May reflect either text or thoughts.
	 */
	def newTextPointer: Changing[String]
	/**
	 * @return A flag that contains true while the LLM is producing "thinking" or reflective content.
	 *         Note: May be true before the type of the produced text content is known.
	 */
	def thinkingFlag: Flag
	
	/**
	  * @return Time when the latest version of this response was originated.
	  */
	def lastUpdated: Instant
	/**
	  * @return A pointer which contains the origination time of the latest version of this response's contents.
	  */
	def lastUpdatedPointer: Changing[Instant]
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this response is still incoming
	  */
	def isStreaming = !isBuffered
	
	
	// OTHER    ------------------------------
	
	/**
	 * Prints all of this reply that has been read so far, and continues appending the text as additions are received.
	 * This method does not block during this printing process.
	 * @param f A mapping function applied to each printed string before printing it. Default = identity.
	 */
	def printAsReceived(f: Mutate[String] = Identity) = {
		println()
		newTextPointer.addContinuousListener { e => print(f(e.newValue)) }
		print(f(textPointer.value))
	}
}
