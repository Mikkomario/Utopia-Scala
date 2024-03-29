package utopia.annex.model.schrodinger

import utopia.access.http.Status.{Forbidden, Unauthorized}
import utopia.annex.model.error.{RequestDeniedException, UnauthorizedRequestException}
import utopia.annex.model.response.RequestNotSent.RequestWasDeprecated
import utopia.annex.model.response.{RequestFailure, RequestResult, Response, ResponseBody}
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.util.StringExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Used for handling results for search operations that may fail. Uses a local placeholder result until server
  * results arrive. Server results will always override the local results, however.
  * @author Mikko Hilpinen
  * @since 18.7.2020, v1
  */
@deprecated("Replaced with PullSchrodinger", "v1.4")
class TryFindSchrodinger[I](localResult: Try[I]) extends Schrodinger[Try[I], Try[I]]
{
	// IMPLEMENTED	--------------------------------
	
	override protected def instanceFrom(result: Option[Try[I]]) = result.getOrElse(localResult)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Completes this schrödinger with the specified response once it arrives (or doesn't)
	  * @param result Asynchronous request result (response or a reason why the request was not sent)
	  * @param parse A function for parsing response body
	  * @param exc Implicit execution context
	  */
	def completeWith(result: Future[RequestResult])(parse: ResponseBody => Try[I])
	                (implicit exc: ExecutionContext) =
	{
		result.onComplete {
			case Success(result) =>
				result match {
					case Response.Success(_, body, _) => complete(parse(body))
					case Response.Failure(status, message, _) =>
						val errorMessage = message.nonEmptyOrElse(s"Received a response with status $status")
						val error = status match {
							case Unauthorized => new UnauthorizedRequestException(errorMessage)
							case Forbidden => new RequestDeniedException(errorMessage)
							case _ => new RequestFailedException(errorMessage)
						}
						complete(Failure(error))
					case RequestWasDeprecated => complete(localResult)
					case f: RequestFailure => complete(f.toFailure)
				}
			case Failure(e) => complete(Failure(e))
		}
	}
}
