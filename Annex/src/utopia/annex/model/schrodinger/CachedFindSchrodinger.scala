package utopia.annex.model.schrodinger

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
}
