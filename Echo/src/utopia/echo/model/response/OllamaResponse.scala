package utopia.echo.model.response

import utopia.annex.model.manifest.HasSchrodingerState
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

/**
  * Common trait / interface for LLM replies, whether they're streamed or buffered
  * and whether they're in chat or response format.
  * @tparam Buffered Buffered version of this response
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait OllamaResponse[+Buffered] extends HasSchrodingerState
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A future that resolves into a buffered / completed version of this response, once read.
	  *         Will yield a failure in case response-parsing failed.
	  */
	def future: Future[Try[Buffered]]
	/**
	  * @return A future that resolves into the final response statistics once they arrive.
	  *         Will contain a failure in case response-parsing / processing failed.
	  */
	def statisticsFuture: Future[Try[ResponseStatistics]]
	
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
	  * @return Time when the latest version of this response was originated.
	  */
	def lastUpdated: Instant
	/**
	  * @return A pointer which contains the origination time of the latest version of this response's contents.
	  */
	def lastUpdatedPointer: Changing[Instant]
}
