package utopia.annex.model.schrodinger

import scala.util.{Failure, Success, Try}

/**
  * This Schrödinger is used when deleting instances from the server side
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait DeleteSchrodinger[G] extends Schrodinger[Try[Unit], Option[G]]
{
	// ABSTRACT	--------------------------------
	
	/**
	  * @return A "ghost" of the deleted item, in case it needs to be considered un-deleted
	  */
	protected def ghost: G
	
	
	// IMPLEMENTED	----------------------------
	
	override protected def instanceFrom(result: Option[Try[Unit]]) =
		if (result.forall { _.isSuccess }) None else Some(ghost)
	
	
	// OTHER    --------------------------------
	
	/**
	  * Marks this deletion schrödinger as successful
	  */
	def succeed() = complete(Success(()))
	
	/**
	  * Marks this deletion schrödinger as failed
	  * @param failure Associated failure
	  */
	def failWith(failure: Failure[Unit]) = complete(failure)
	
	/**
	  * Marks this deletion schrödinger as failed
	  * @param error Associated failure
	  */
	def failWith(error: Throwable): Unit = failWith(Failure(error))
}
