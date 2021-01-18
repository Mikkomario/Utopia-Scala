package utopia.annex.model.schrodinger

import utopia.annex.model.response.RequestNotSent

/**
  * This schrödinger item is used in requests where results are searched first from local, then global data
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait PositiveFindSchrodinger[S, I] extends Schrodinger[Either[RequestNotSent, Option[I]], Option[S]]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return Local search result
	  */
	protected def localResult: Option[S]
	
	/**
	  * @param instance Instance received from server side
	  * @return A spirit representation of that instance
	  */
	protected def spiritOf(instance: I): S
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def instanceFrom(result: Option[Either[RequestNotSent, Option[I]]]) = result match
	{
		case Some(serverResult) =>
			serverResult match
			{
				case Right(instance) => instance.map(spiritOf)
				case Left(_) => localResult
			}
		case None => localResult
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Completes this schrödinger with a failure result
	  * @param notSent Failure (request was not sent)
	  */
	def failWith(notSent: RequestNotSent) = complete(Left(notSent))
	
	/**
	  * Completes this schrödinger with a successful server response
	  * @param searchResult Server search response
	  */
	def succeedWith(searchResult: Option[I]) = complete(Right(searchResult))
	
	/**
	  * Completes this schrödinger with a successful search result
	  * @param result Server search result
	  */
	def succeedWithFoundResult(result: I) = succeedWith(Some(result))
	
	/**
	  * Completes this schrödinger by indicating that server response was successful but no item was found
	  */
	def succeedWithNoResultsFound() = succeedWith(None)
}
