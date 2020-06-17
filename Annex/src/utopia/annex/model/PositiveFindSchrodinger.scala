package utopia.annex.model

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
}
