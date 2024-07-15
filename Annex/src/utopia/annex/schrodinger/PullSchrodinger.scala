package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Final, Flux, PositiveFlux}
import utopia.annex.model.response.{RequestResult, RequestResult2}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PullSchrodinger
{
	// ATTRIBUTES   ------------------------
	
	private lazy val pendingResultFailure = Failure(new IllegalStateException("Remote result is still pending"))
	
	
	// OTHER    ----------------------------
	
	/**
	  * Wraps a pointer
	  * @param pointer A pointer that contains 3 elements:
	  *                 1) Current manifest (any result)
	  *                 2) Current result (placeholder until a result is acquired)
	  *                 3) State (final or flux)
	  * @tparam L Type of locally cached items (spirits)
	  * @tparam R Type of remotely read items (instances)
	  * @return A new schrödinger
	  */
	def wrap[L, R](pointer: Changing[(Try[L], Try[R], SchrodingerState)]) = new PullSchrodinger[L, R](pointer)
	
	/**
	  * Creates a schrödinger that has already resolved into either a success or a failure
	  * @param result Pull result (success or failure)
	  * @tparam A Type of pulled item, when successful
	  * @return Either an alive (success) or a dead (failure) schrödinger
	  */
	def resolved[A](result: Try[A]) = wrap(Fixed(result, result, Final(result.isSuccess)))
	/**
	  * Creates a schrödinger that has already resolved successfully
	  * @param item The pulled item
	  * @tparam A Type of the pulled item
	  * @return A new static schrödinger
	  */
	def successful[A](item: A) = resolved(Success(item))
	/**
	  * Creates a new schrödinger that has already failed
	  * @param cause Cause of failure
	  * @tparam A Type of the pulled item, if this schrödinger were successful
	  * @return A new failed schrödinger
	  */
	def failed[A](cause: Throwable) = resolved[A](Failure(cause))
	
	/**
	  * Pulls a local and a remote instance.
	  * Stores the local instance (if available) until a remote instance is acquired.
	  * Expects a non-empty response from the server.
	  * Typically used in situations where locally cached data may act as a placeholder until server results arrive.
	  * @param local        Locally cached data. None if no local data is available (which is an impermanent failure case).
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser       A parser used for parsing items from a server response
	  * @param localize     A function that converts a remotely read instance into its local variant
	  * @param exc          Implicit execution context
	  * @tparam L Type of locally cached items (spirits)
	  * @tparam R Type of remotely read items (instances)
	  * @return A new schrödinger that contains local data as a placeholder until server results arrive.
	  *         Dead if no item could be read from the server response
	  *         (whether the request itself was successful or not)
	  */
	def apply[L, R](local: Option[L], resultFuture: Future[RequestResult], parser: FromModelFactory[R])
	               (localize: R => L)(implicit exc: ExecutionContext) =
		_apply(local.toTry { new NoSuchElementException("No local data") }, pendingResultFailure, resultFuture, parser,
			Flux(local.isDefined))(localize)
	/**
	  * Pulls a local and a remote instance.
	  * Stores the local instance (if available) until a remote instance is acquired.
	  * Expects a non-empty response from the server.
	  * Typically used in situations where locally cached data may act as a placeholder until server results arrive.
	  * @param local        Locally cached data. None if no local data is available (which is an impermanent failure case).
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param localize     A function that converts a remotely read instance into its local variant
	  * @param exc          Implicit execution context
	  * @tparam L Type of locally cached items (spirits)
	  * @tparam R Type of remotely read items (instances)
	  * @return A new schrödinger that contains local data as a placeholder until server results arrive.
	  *         Dead if no item could be read from the server response
	  *         (whether the request itself was successful or not)
	  */
	def apply2[L, R](local: Option[L], resultFuture: Future[RequestResult2[R]])
	                (localize: R => L)(implicit exc: ExecutionContext) =
		_apply2(local.toTry { new NoSuchElementException("No local data") }, pendingResultFailure, resultFuture,
			Flux(local.isDefined))(localize)
	
	/**
	  * Pulls a local instance if accessible. If not, pulls the remote instance instead.
	  * In other words, remote pull is performed only if no local data is found.
	  * Expects one of these pulls to yield an instance (i.e. case where no data is acquired is considered a failure).
	  * This is typically used when data is expected to exist either locally or remotely and local data,
	  * if present, is considered accurate enough to represent the final state.
	  * @param local       Local pull results (Some or None)
	  * @param parser      Parser used for parsing remote pull results
	  * @param makeRequest A function that starts the remote pull. Yields a request result.
	  * @param exc         Implicit execution context
	  * @tparam A Type of pull results, when acquired
	  * @return A new schrödinger, either based on local data (final) or remote pull (flux)
	  */
	def any[A](local: Option[A], parser: FromModelFactory[A])(makeRequest: => Future[RequestResult])
	          (implicit exc: ExecutionContext) =
	{
		local match {
			// Case: Local results found => Skips the remote search
			case Some(found) => successful(found)
			// Case: No local results => Performs the remote search
			case None => apply(local, makeRequest, parser)(Identity)
		}
	}
	/**
	  * Pulls a local instance if accessible. If not, pulls the remote instance instead.
	  * In other words, remote pull is performed only if no local data is found.
	  * Expects one of these pulls to yield an instance (i.e. case where no data is acquired is considered a failure).
	  * This is typically used when data is expected to exist either locally or remotely and local data,
	  * if present, is considered accurate enough to represent the final state.
	  * @param local       Local pull results (Some or None)
	  * @param makeRequest A function that starts the remote pull. Yields a request result.
	  * @param exc         Implicit execution context
	  * @tparam A Type of pull results, when acquired
	  * @return A new schrödinger, either based on local data (final) or remote pull (flux)
	  */
	def any2[A](local: Option[A])(makeRequest: => Future[RequestResult2[A]])
	           (implicit exc: ExecutionContext) =
	{
		local match {
			// Case: Local results found => Skips the remote search
			case Some(found) => successful(found)
			// Case: No local results => Performs the remote search
			case None => apply2(local, makeRequest)(Identity)
		}
	}
	
	/**
	  * Creates a schrödinger that wraps a remote pull request (single instance).
	  * Expects the request to succeed and to yield exactly one instance.
	  * This is typically used when no data has been cached locally, or when local data is not appropriate to be used.
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser       A parser used for parsing items from a server response
	  * @param exc          Implicit execution context
	  * @tparam A Type of the read item, when successful
	  * @return A new schrödinger that contains a failure until server results arrive.
	  *         Once the results arrive, contains parsing results
	  *         (failure if request or parsing failed, or if no items were found, success otherwise)
	  */
	def remote[A](resultFuture: Future[RequestResult], parser: FromModelFactory[A])
	                 (implicit exc: ExecutionContext) =
		_apply(pendingResultFailure, pendingResultFailure, resultFuture, parser, PositiveFlux)(Identity)
	/**
	  * Creates a schrödinger that wraps a remote pull request (single instance).
	  * Expects the request to succeed and to yield exactly one instance.
	  * This is typically used when no data has been cached locally, or when local data is not appropriate to be used.
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param exc          Implicit execution context
	  * @tparam A Type of the read item, when successful
	  * @return A new schrödinger that contains a failure until server results arrive.
	  *         Once the results arrive, contains parsing results
	  *         (failure if request or parsing failed, or if no items were found, success otherwise)
	  */
	def remote2[A](resultFuture: Future[RequestResult2[A]])(implicit exc: ExecutionContext) =
		_apply2(pendingResultFailure, pendingResultFailure, resultFuture, PositiveFlux)(Identity)
	
	private def _apply[L, R](initialManifest: Try[L], placeHolderResult: Try[R], resultFuture: Future[RequestResult],
	                         parser: FromModelFactory[R], flux: Flux = Flux)(localize: R => L)
	                        (implicit exc: ExecutionContext) =
	{
		val p = Schrodinger.makePointer[Try[L], Try[R]](initialManifest, placeHolderResult, resultFuture, flux) {
			case Right(body) => body.tryParseSingleWith(parser)
			case Left(error) => Failure(error)
		} { (placeHolder, parsed) =>
			val manifest = parsed match {
				case Success(res) => Success(localize(res))
				case Failure(error) => placeHolder.orElse { Failure(error) }
			}
			(manifest, parsed, Final(parsed.isSuccess))
		}
		wrap(p)
	}
	private def _apply2[L, R](initialManifest: Try[L], placeHolderResult: Try[R],
	                          resultFuture: Future[RequestResult2[R]], flux: Flux = Flux)
	                         (localize: R => L)
	                         (implicit exc: ExecutionContext) =
	{
		val p = Schrodinger.makePointer2[Try[L], R, Try[R]](
			initialManifest, placeHolderResult, resultFuture, flux)(Identity){ (placeHolder, parsed) =>
			val manifest = parsed match {
				case Success(res) => Success(localize(res))
				case Failure(error) => placeHolder.orElse { Failure(error) }
			}
			(manifest, parsed, Final(parsed.isSuccess))
		}
		wrap(p)
	}
}

/**
  * A schrödinger that represents an attempt to retrieve the data of exactly one item from local and/or remote
  * data. Will contain a failure until data is acquired.
  * @author Mikko Hilpinen
  * @since 23.12.2022, v1.4
  * @tparam L Type of locally cached items (spirits)
  * @tparam R Type of remotely read items (instances)
  */
class PullSchrodinger[+L, +R](pointer: Changing[(Try[L], Try[R], SchrodingerState)])
	extends Schrodinger[Try[L], Try[R]](pointer)
