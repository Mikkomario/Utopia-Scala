package utopia.annex.model.schrodinger

import scala.util.{Success, Try}

/**
  * This schrödinger item is used when posting new data to the server
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait PostSchrodinger[S, I] extends Schrodinger[Try[I], Try[S]]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return The spirit being posted to the server
	  */
	protected def postSpirit: S
	
	/**
	  * @param instance A server-originated instance
	  * @return A spirit representation of that instance
	  */
	protected def spiritOf(instance: I): S
	
	
	// IMPLEMENTED	---------------------
	
	override protected def instanceFrom(result: Option[Try[I]]) = result match
	{
		case Some(serverResult) => serverResult.map(spiritOf)
		case None => Success(postSpirit)
	}
}
