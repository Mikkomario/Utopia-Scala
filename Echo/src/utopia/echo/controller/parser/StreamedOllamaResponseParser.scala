package utopia.echo.controller.parser

import utopia.echo.model.response.{OllamaResponse, ResponseStatistics}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.view.mutable.eventful.LockablePointer
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

/**
  * A response parser which parses LLM replies from a streamed json response.
  * Expects the stream to contain newline-delimited json where each line represents a json object.
  * @tparam A Type of the (streamed) responses parsed
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait StreamedOllamaResponseParser[A <: OllamaResponse[_]] extends StreamedResponseParser[A, ResponseStatistics]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param response Response model to read
	  * @return Incremented text from the specified response
	  */
	protected def textFromResponse(response: Model): String
	
	/**
	  * @param textPointer Pointer that will contain all response text (built incrementally)
	  * @param lastUpdatedPointer Pointer that contains the origin time of the latest response update
	  * @param statisticsFuture Future that eventually resolves into statistics concerning this response,
	  *                         or to a failure if response or json processing fails.
	  * @return Completed streamed response instance
	  */
	protected def responseFrom(textPointer: Changing[String], lastUpdatedPointer: Changing[Instant],
	                           statisticsFuture: Future[Try[ResponseStatistics]]): A
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def newParser: SingleStreamedResponseParser[A, ResponseStatistics] =
		new SingleOllamaResponseParser
	
	override protected def failureMessageFrom(response: A): String = response.text
	
	
	// NESTED   ------------------------
	
	private class SingleOllamaResponseParser extends SingleStreamedResponseParser[A, ResponseStatistics]
	{
		// ATTRIBUTES   ----------------
		
		// Prepares a pointer to store the read reply text
		// TODO: May need Volatile features here
		private val textPointer = new LockablePointer[String]("")
		private val lastUpdatedPointer = new LockablePointer[Instant](Now)
		
		
		// IMPLEMENTED  ----------------
		
		override def updateStatus(response: Model): Unit = {
			val newText = textFromResponse(response)
			// Appends the read text to the text pointer
			textPointer.update { _ + newText }
			lastUpdatedPointer.value = response("created_at").getInstant
		}
		
		// Converts the last read model into response statistics, if possible
		override def processFinalParseResult(finalResponse: Try[Model]): Try[ResponseStatistics] =
			finalResponse.map(ResponseStatistics.fromOllamaResponse)
		
		override def finish(): Unit = {
			// Locks the pointers - There shall not be any updates afterwards
			textPointer.lock()
			lastUpdatedPointer.lock()
		}
		
		override def responseFrom(future: Future[Try[ResponseStatistics]]): A =
			StreamedOllamaResponseParser.this.responseFrom(textPointer.readOnly, lastUpdatedPointer.readOnly, future)
	}
}
