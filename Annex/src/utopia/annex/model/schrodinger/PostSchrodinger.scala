package utopia.annex.model.schrodinger

import scala.util.{Failure, Success, Try}

/**
  * This schrödinger item is used when posting new data to the server
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
@deprecated("Replaced with a new version in utopia.annex.schrodinger", "v1.4")
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
	
	
	// OTHER    ------------------------
	
	/**
	  * Successfully completes this schrödinger instance
	  * @param successResult Successful result
	  */
	def succeedWith(successResult: I) = complete(Success(successResult))
	
	/**
	  * Completes this Sscrödinger with a failure result
	  * @param failureResult A failure result
	  */
	def failWith(failureResult: Failure[I]) = complete(failureResult)
	
	/**
	  * Completes this Sscrödinger with a failure result
	  * @param error Error assigned as failure
	  */
	def failWith(error: Throwable): Unit = failWith(Failure(error))
}
