package utopia.annex.model.schrodinger

import utopia.disciple.model.error.RequestFailedException
import utopia.annex.model.response.RequestNotSent.RequestFailed
import utopia.annex.model.response.{RequestNotSent, Response, ResponseBody}
import utopia.flow.util.CollectionExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * This Shcrödinger item is used when previous results may be cached already. In which case presents them as
  * placeholder result until server response is received.
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  */
class CachedFindSchrodinger[I](cached: I) extends Schrodinger[Try[I], I]
{
	// IMPLEMENTED	-------------------------------
	
	override protected def instanceFrom(result: Option[Try[I]]) = result match
	{
		case Some(result) =>
			result match
			{
				case Success(instance) => instance
				case Failure(_) => cached
			}
		case None => cached
	}
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Completes this schrodinger by handling a request response once one is acquired
	  * @param result A future containing the request result (either a server response or a reason why request wasn't sent)
	  * @param parse A function for parsing response contents (needs to handle the case where response body is empty)
	  * @param recordError A function that is called when possible errors are encountered (default = ignore errors)
	  * @param exc Implicit execution context
	  */
	def completeWith(result: Future[Either[RequestNotSent, Response]])(parse: ResponseBody => Try[I])
					(recordError: Throwable => Unit = _ => ())(implicit exc: ExecutionContext) =
	{
		result.onComplete {
			case Success(result) =>
				result match {
					case Right(response) =>
						response match
						{
							case Response.Success(_, body, _) =>
								val parseResult = parse(body)
								parseResult.failure.foreach(recordError)
								complete(parseResult)
							case Response.Failure(status, message, _) =>
								val errorMessage = message match
								{
									case Some(m) => s"Invitation retrieval failed ($status). Response message: $m"
									case None => s"Invitation retrieval failed with status $status"
								}
								val error = new RequestFailedException(errorMessage)
								recordError(error)
								complete(Failure(error))
						}
					case Left(notSent) =>
						notSent match
						{
							case RequestFailed(error) =>
								recordError(error)
								complete(Failure(error))
							case _ => complete(Failure(
								new RequestFailedException("Request was deprecated before it could be sent")))
						}
				}
			case Failure(error) =>
				recordError(error)
				complete(Failure(error))
		}
	}
}
