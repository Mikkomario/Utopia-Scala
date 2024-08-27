package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Final, PositiveFlux}
import utopia.annex.model.response.RequestResult
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object DeleteSchrodinger
{
	// ATTRIBUTES   -------------------
	
	private lazy val successValue = Success(())
	
	/**
	  * A successfully resolved schrödinger
	  */
	lazy val successful = wrap(Fixed((None, successValue, Alive)))
	
	
	// OTHER    ------------------------
	
	/**
	  * Wraps a pointer into a schrödinger
	  * @param pointer A pointer to wrap, always contains 3 elements:
	  *                 1) The item being deleted, but only if the deletion has failed
	  *                 2) Request result (success while pending)
	  *                 3) State (flux, alive, dead)
	  * @tparam A Type of the item being deleted
	  * @return A new schrödinger
	  */
	def wrap[A](pointer: Changing[(Option[A], Try[Unit], SchrodingerState)]) =
		new DeleteSchrodinger[A](pointer)
	
	/**
	  * Creates a schrödinger that represents a failed attempt to delete an instance
	  * @param instance The instance that was being deleted
	  * @param cause Cause of failure
	  * @tparam A Type of the specified instance
	  * @return A new schrödinger
	  */
	def failed[A](instance: A, cause: Throwable) =
		wrap(Fixed((Some(instance), Failure(cause), Dead)))
	
	/**
	  * Creates a schrödinger that represents an ongoing attempt to delete a remote instance.
	  * Presents the targeted instance as the manifest, but only if deletion fails.
	  * @param ghost The local representation of the remote instance that's being deleted.
	  *              Call-by-name, only called if the request fails and the instance needs to be brought back.
	  * @param resultFuture A future that resolves into server's response, if successful
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation for handling certain unexpected failures.
	  *            For example those thrown by listeners attached to this Schrödinger's pointers.
	  * @tparam A Type of instance being deleted
	  * @return A new schrödinger
	  */
	def apply[A](ghost: => A, resultFuture: Future[RequestResult[Any]])(implicit exc: ExecutionContext, log: Logger) =
		wrap(Schrodinger.makePointer[Option[A], Any, Try[Unit]](None, successValue, resultFuture,
			PositiveFlux) { _.map { _ => () } } {
			(_, result) => (if (result.isSuccess) None else Some(ghost), result, Final(result.isSuccess)) })
}

/**
  * A schrödinger that represents an attempt to delete a remote instance, which has been cached locally.
  * Brings the original item back as the manifest in case the request fails.
  * @author Mikko Hilpinen
  * @since 24.12.2022, v1.4
  */
class DeleteSchrodinger[+A](pointer: Changing[(Option[A], Try[Unit], SchrodingerState)])
	extends Schrodinger[Option[A], Try[Unit]](pointer)
