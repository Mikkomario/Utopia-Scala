package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Final, PositiveFlux}
import utopia.annex.model.response.{RequestResult, RequestResult2}
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PostSchrodinger
{
	// ATTRIBUTES   ------------------------
	
	private lazy val pendingFailure = Failure(new IllegalStateException("Posting is still in progress"))
	
	
	// OTHER    ----------------------------
	
	/**
	  * Wraps a pointer into a schrödinger
	  * @param pointer A pointer that always contains 3 elements:
	  *                 1) The spirit / essence of the posted or received item (aka. manifest)
	  *                 2) The post result (failure until resolved)
	  *                 3) State (flux, alive, dead)
	  * @tparam S Type of the spirit / manifest used
	  * @tparam A Type of the remote instance being created
	  * @return A new schrödinger
	  */
	def wrap[S, A](pointer: Changing[(S, Try[A], SchrodingerState)]) = new PostSchrodinger[S, A](pointer)
	
	/**
	  * Creates a new schrödinger that represents a completed successful attempt to create a remote instance
	  * @param instance Instance that was created
	  * @tparam A Type of the instance
	  * @return A new schrödinger
	  */
	def successful[A](instance: A) = wrap(Fixed((instance, Success(instance), Alive)))
	/**
	  * Creates a new schrödinger that represents a (permanently) failed attempt to create a remote instance
	  * @param spirit Manifest of the data that was posted
	  * @param cause Cause of failure
	  * @tparam S Type of spirit / manifest
	  * @tparam A Type of the instance, if one would have been created
	  * @return A new schrödinger
	  */
	def failed[S, A](spirit: S, cause: Throwable) =
		wrap[S, A](Fixed((spirit, Failure(cause), Dead)))
	
	/**
	  * Creates a new schrödinger that represents an ongoing attempt to create a remote instance.
	  * While the request is pending, local posted data (aka. the spirit) is being presented as the manifest.
	  * Once the request resolves the parsed item and its "spirit" form are presented.
	  * @param spirit The data being posted
	  * @param resultFuture A future of the server's response to the post attempt
	  * @param parser A parser that will interpret the response body
	  * @param spiritualize A function for presenting a "spirit" form / manifest from the received response,
	  *                     if successful
	  * @param exc Implicit execution context
	  * @tparam S Type of spirit / manifest used
	  * @tparam A Type of created instance
	  * @return A new schrödinger
	  */
	def apply[S, A](spirit: S, resultFuture: Future[RequestResult], parser: FromModelFactory[A])
	               (spiritualize: A => S)(implicit exc: ExecutionContext) =
		wrap(Schrodinger.makePointer[S, Try[A]](spirit, pendingFailure, resultFuture, PositiveFlux) {
			case Right(body) => body.parsedSingle(parser)
			case Left(error) => Failure(error)
		} { (spirit, result) =>
			val manifest = result match {
				case Success(res) => spiritualize(res)
				case Failure(_) => spirit
			}
			(manifest, result, Final(result.isSuccess))
		})
	/**
	  * Creates a new schrödinger that represents an ongoing attempt to create a remote instance.
	  * While the request is pending, local posted data (aka. the spirit) is being presented as the manifest.
	  * Once the request resolves the parsed item and its "spirit" form are presented.
	  * @param spirit The data being posted
	  * @param resultFuture A future of the server's response to the post attempt
	  * @param spiritualize A function for presenting a "spirit" form / manifest from the received response,
	  *                     if successful
	  * @param exc Implicit execution context
	  * @tparam S Type of spirit / manifest used
	  * @tparam A Type of created instance
	  * @return A new schrödinger
	  */
	def apply2[S, A](spirit: S, resultFuture: Future[RequestResult2[A]])(spiritualize: A => S)
	                (implicit exc: ExecutionContext) =
		wrap(Schrodinger.makePointer2[S, A, Try[A]](spirit, pendingFailure, resultFuture, PositiveFlux)(Identity){
			(spirit, result) =>
				val manifest = result match {
					case Success(res) => spiritualize(res)
					case Failure(_) => spirit
				}
				(manifest, result, Final(result.isSuccess))
		})
}

/**
  * A schrödinger that represents an attempt to push data to a remote server.
  * Presents the posted data until validation (and fulfilled data) arrives
  * @author Mikko Hilpinen
  * @since 24.12.2022, v1.4
  * @tparam S Type of the data being posted (local data, aka. spirit)
  * @tparam A Type of data after it has been instantiated on server-side (remote data, aka. instance)
  */
class PostSchrodinger[+S, +A](pointer: Changing[(S, Try[A], SchrodingerState)]) extends Schrodinger[S, Try[A]](pointer)
