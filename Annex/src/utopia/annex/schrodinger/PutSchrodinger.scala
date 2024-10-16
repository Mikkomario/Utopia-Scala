package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Final, PositiveFlux}
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Identity
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PutSchrodinger
{
	/**
	  * Wraps a pointer into a schrödinger
	  * @param pointer A pointer to wrap, which always contains 3 elements:
	  *                 1) The expected final instance state (simulated until results arrive)
	  *                 2) Request result (success or failure, simulated success while pending)
	  *                 3) State (flux, alive, dead)
	  * @tparam A Type of modified remote item
	  * @return A new schrödinger
	  */
	def wrap[A](pointer: Changing[(A, Try[A], SchrodingerState)]) = new PutSchrodinger[A](pointer)
	
	/**
	  * Creates a schrödinger that has already resolved into a success
	  * @param modified A successfully modified (remote) item
	  * @tparam A Type of the specified item
	  * @return A new schrödinger that won't change
	  */
	def successful[A](modified: A) = wrap(Fixed((modified, Success(modified), Alive)))
	/**
	  * Creates a schrödinger that represents a failed attempt to modify an instance
	  * @param original The instance on which modifications were attempted, presented in its original (unmodified) form
	  * @param cause Cause of failure
	  * @tparam A Type of the specified instance
	  * @return A new schrödinger that won't change
	  */
	def failed[A](original: A, cause: Throwable) = wrap(Fixed(original, Failure(cause), Dead))
	
	/**
	  * Creates a new schrödinger that represents an attempt to modify a remote instance.
	  * Presents simulated results, based on a modified local instance, until actual results arrive.
	  * If the request fails, reverts the manifest back to the original item.
	  * @param original The item in its original form (call-by-name)
	  * @param modifiedLocal The modified local item (simulating the resulting item)
	  * @param resultFuture A future that resolves into server response, if successful
	  * @param exc Implicit execution context
	  * @tparam A Type of instance being modified
	  * @return A new schrödinger
	  */
	def apply[A](original: => A, modifiedLocal: A, resultFuture: Future[RequestResult[A]])
	            (implicit exc: ExecutionContext, log: Logger) =
		wrap(Schrodinger.makePointer[A, A, Try[A]](modifiedLocal, Success(modifiedLocal), resultFuture,
			PositiveFlux)(Identity){ (_, result) => (result.getOrElse(original), result, Final(result.isSuccess)) })
	
	/**
	  * Creates a new schrödinger that represents an attempt to modify a remote instance.
	  * Presents simulated results, based on a modified local instance, until actual results arrive.
	  * If the request fails, reverts the manifest back to the original item.
	  * @param original The item in its original form (call-by-name)
	  * @param modifiedLocal The modified local item (simulating the resulting item)
	  * @param resultFuture A future that resolves into server response, if successful
	  * @param parser A parser used for interpreting the response body
	  * @param exc Implicit execution context
	  * @tparam A Type of instance being modified
	  * @return A new schrödinger
	  */
	def putAndParse[A](original: => A, modifiedLocal: A, resultFuture: Future[RequestResult[Value]],
	                   parser: FromModelFactory[A])
	                  (implicit exc: ExecutionContext, log: Logger) =
		apply(original, modifiedLocal, resultFuture.map { _.parsingOneWith(parser) })
	
	@deprecated("Deprecated for removal. Please use .putAndParse(...) instead", "v1.8")
	def apply[A](original: => A, modifiedLocal: A, resultFuture: Future[RequestResult[Value]],
	             parser: FromModelFactory[A])
	            (implicit exc: ExecutionContext, log: Logger): PutSchrodinger[A] =
		putAndParse(original, modifiedLocal, resultFuture, parser)
}

/**
  * A schrödinger that represents an attempt to modify a remote instance.
  * Presents local, modified data (a spirit) until the request resolves.
  * After this presents the received (modified) data - or the original data in case the modifications were rejected.
  * @author Mikko Hilpinen
  * @since 24.12.2022, v1.4
  * @tparam A Type of the (remote) instance that's being modified
  */
class PutSchrodinger[+A](pointer: Changing[(A, Try[A], SchrodingerState)]) extends Schrodinger(pointer)
