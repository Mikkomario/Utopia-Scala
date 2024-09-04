package utopia.echo.controller.parser

import utopia.flow.generic.model.immutable.Model

import scala.concurrent.Future
import scala.util.Try

/**
  * A response parser which parses a single streamed json response.
  * Expects the stream to contain newline-delimited json where each line represents a json object.
  * @tparam R Type of the (streamed) responses parsed
  * @tparam V Type of asynchronously acquired value utilized
  * @author Mikko Hilpinen
  * @since 3.9.2024, v1.1
  */
trait SingleStreamedResponseParser[+R, V]
{
	// ABSTRACT ---------------------------
	
	/**
	  * Updates the current status based on a parsed response object
	  * @param response Parsed response object
	  */
	def updateStatus(response: Model): Unit
	/**
	  * This method is called once all response objects have been read
	  * @param finalResponse The last parsed response model. Failure if parsing failed.
	  *                      If successful, this model has already been presented to [[updateStatus]](...).
	  * @return Value to complete the managed future with. Maybe a failure.
	  */
	def processFinalParseResult(finalResponse: Try[Model]): Try[V]
	/**
	  * This method is called once the response-parsing process is about to complete.
	  * After this method call [[updateStatus]](...) or [[processFinalParseResult]](...) won't be called anymore.
	  */
	def finish(): Unit
	
	/**
	  * This method constructs an automatically updating response body.
	  * It is expected to return immediately and not wait for 'future' to complete.
	  *
	  * @param future A future that will contain the result of [[processFinalParseResult]](...)
	  *               once all response models have been read.
	  * @return A request response body to return
	  */
	def responseFrom(future: Future[Try[V]]): R
}
