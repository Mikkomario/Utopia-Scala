package utopia.annex.model.schrodinger

import scala.util.{Failure, Success, Try}

/**
  * This schrödinger item is used when modifing instances at the server
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait PutSchrodinger[S, I] extends Schrodinger[Try[I], S]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Local item representation without proposed modifications
	  */
	protected def localOriginal: S
	
	/**
	  * @return Modified version of the local item
	  */
	protected def localModified: S
	
	/**
	  * @param instance A server-side instance
	  * @return A spirit representation of the instance
	  */
	protected def spiritOf(instance: I): S
	
	
	// IMPLEMENTED	---------------------
	
	override protected def instanceFrom(result: Option[Try[I]]) = result match
	{
		case Some(serverResult) =>
			serverResult match
			{
				case Success(instance) => spiritOf(instance)
				case Failure(_) => localOriginal
			}
		case None => localModified
	}
}
