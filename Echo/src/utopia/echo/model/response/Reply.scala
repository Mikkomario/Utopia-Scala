package utopia.echo.model.response

import utopia.annex.model.manifest.HasSchrodingerState
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

/**
  * Common trait / interface for LLM replies, whether they're streamed or buffered
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait Reply extends HasSchrodingerState
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A future that resolves into a buffered / completed version of this reply once read.
	  *         Will yield a failure in case reply-parsing failed.
	  */
	def future: Future[Try[BufferedReply]]
	/**
	  * @return A future that resolves into the final response statistics once they arrive.
	  *         Will contain a failure in case reply-parsing / processing failed.
	  */
	def statisticsFuture: Future[Try[ResponseStatistics]]
	
	/**
	  * @return The current text in this reply.
	  *         Note: Might not be final.
	  */
	def text: String
	/**
	  * @return A pointer which contains the currently built reply text
	  */
	def textPointer: Changing[String]
	
	/**
	  * @return Time when the latest version of this reply was originated.
	  */
	def lastUpdated: Instant
	/**
	  * @return A pointer which contains the origination time of the latest version of this reply's contents.
	  */
	def lastUpdatedPointer: Changing[Instant]
}
