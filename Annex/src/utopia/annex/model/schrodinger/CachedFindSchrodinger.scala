package utopia.annex.model.schrodinger

import utopia.annex.model.response.RequestNotSent

/**
  * This Shcrödinger item is used when previous results may be cached already. In which case presents them as
  * placeholder result until server response is received.
  * @author Mikko Hilpinen
  * @since 12.7.2020, v1
  */
class CachedFindSchrodinger[I](cached: I) extends Schrodinger[Either[RequestNotSent, I], I]
{
	// IMPLEMENTED	-------------------------------
	
	override protected def instanceFrom(result: Option[Either[RequestNotSent, I]]) = result match
	{
		case Some(result) =>
			result match
			{
				case Right(instance) => instance
				case Left(_) => cached
			}
		case None => cached
	}
}
