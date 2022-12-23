package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Final, Flux}
import utopia.annex.model.manifest.{HasSchrodingerState, Manifest, SchrodingerState}
import utopia.annex.model.response.ResponseBody.Content
import utopia.annex.model.response.{RequestFailure, RequestResult, Response, ResponseBody}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.operator.Identity
import utopia.flow.operator.MaybeEmpty.HasIsEmpty
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

object Schrodinger2
{
	def wrap[M, R](pointer: Changing[(M, R, SchrodingerState)]) = new Schrodinger2[M, R](pointer)
	def static[M, R](manifest: M, result: R, state: Final) = wrap(Fixed(manifest, result, state))
	def success[M, R](manifest: M, result: R) = static(manifest, result, Alive)
	def failure[M, R](manifest: M, result: R) = static(manifest, result, Dead)
	
	// TODO: Move find functions to a separate class
	/**
	  * Creates a schrödinger that contains 0-1 locally cached values until server results arrive,
	  * from which a new item is parsed, if present and possible.
	  * Typically used when updating locally cached data.
	  * @param local        A result based on local data pull (0-n items as a Vector)
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser       A parser used for parsing items from a server response
	  * @param emptyIsDead  Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                     Default = false = Result is alive as long as it resolves successfully.
	  * @param exc          Implicit execution context
	  * @tparam L Type of local search results
	  * @tparam R Type of remote search results
	  * @return A new schrödinger that contains local pull results
	  *         and updates itself once the server-side results arrive.
	  */
	def find[L, R](local: Option[L], resultFuture: Future[RequestResult],
	               parser: FromModelFactory[R], emptyIsDead: Boolean = false)(localize: R => L)
	              (implicit exc: ExecutionContext) =
		_get[Option[L], Option[R]](local, None, resultFuture, emptyIsDead) {
			case c: Content => c.parsedSingle(parser).map { Some(_) }
			case _ => Success(None)
		} { _.map(localize) }
	/**
	  * Creates a schrödinger that either **permanently** contains the local search result, if found, or
	  * performs a remote search and contains the server results once they arrive.
	  * In other words, remote search is only performed if no local results are found.
	  * This type of schrödinger is typically used when the local data is expected to be accurate, when present.
	  * @param local Local search results
	  * @param parser A parser used for handling server-side results
	  * @param emptyIsDead Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                    Default = false = Result is alive as long as it resolves successfully.
	  * @param makeRequest A function for starting a remote search.
	  *                    Yields a future that contains the result of this request.
	  * @param exc Implicit execution context
	  * @tparam A Type of search results, when found
	  * @return A new schrödinger, either based on local results (final) or remote search (flux)
	  */
	def findAny[A](local: Option[A], parser: FromModelFactory[A], emptyIsDead: Boolean = false)
	              (makeRequest: => Future[RequestResult])
	              (implicit exc: ExecutionContext) =
	{
		// Case: Local results found => Skips the remote search
		if (local.isDefined)
			success(local, Success(local))
		// Case: No local results => Performs the remote search
		else
			find(local, makeRequest, parser, emptyIsDead)(Identity)
	}
	/**
	  * Creates a schrödinger that wraps a remote search request which yields 0-1 items when successful.
	  * This is typically used when no appropriate local data is available.
	  * @param resultFuture  A future that will contain the server response, if successful
	  * @param parser        A parser used for parsing items from a server response
	  * @param emptyIsDead   Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                      Default = false = Result is alive as long as it resolves successfully.
	  * @param expectFailure Whether the request is expected to fail (default = false = expected to succeed)
	  * @param exc           Implicit execution context
	  * @tparam A Type of found item
	  * @return A new schrödinger that will contain None until server results arrive
	  */
	def findRemote[A](resultFuture: Future[RequestResult], parser: FromModelFactory[A], emptyIsDead: Boolean = false,
	                  expectFailure: Boolean = false)
	                 (implicit exc: ExecutionContext) =
		_getRemote[Option[A]](None, resultFuture, emptyIsDead, expectFailure) {
			case c: Content => c.parsedSingle(parser).map { Some(_) }
			case _ => Success(None)
		}
	
	private def _get[L <: HasIsEmpty, R <: HasIsEmpty](local: L, placeHolder: R, resultFuture: Future[RequestResult],
	                                                emptyIsDead: Boolean = false)
	                                               (parse: ResponseBody => Try[R])(localize: R => L)
	                                               (implicit exc: ExecutionContext) =
		wrap(getPointer(local, placeHolder, resultFuture, emptyIsDead)(parse)(localize: R => L))
	private def _getRemote[M <: { def isEmpty: Boolean }](placeHolder: M, resultFuture: Future[RequestResult],
	                                                      emptyIsDead: Boolean = false, expectFailure: Boolean = false)
	                                                     (parse: ResponseBody => Try[M])
	                                                     (implicit exc: ExecutionContext) =
		wrap(remoteGetPointer(placeHolder, resultFuture, emptyIsDead, expectFailure)(parse))
	
	private[schrodinger] def getPointer[L <: HasIsEmpty, R <: HasIsEmpty](local: L, placeHolder: R,
	                                                                      resultFuture: Future[RequestResult],
	                                                                      emptyIsDead: Boolean = false)
	                                                                     (parse: ResponseBody => Try[R])
	                                                                     (localize: R => L)
	                                                                     (implicit exc: ExecutionContext) =
	{
		makePointer[L, Try[R]](local, Success(placeHolder), resultFuture, Flux(!emptyIsDead || !local.isEmpty)) {
			case Right(body) => parse(body)
			case Left(error) => Failure(error)
		} { (placeHolder, parsed) => testEmptyState(placeHolder, parsed, emptyIsDead)(localize) }
	}
	private[schrodinger] def remoteGetPointer[M <: HasIsEmpty](placeHolder: M, resultFuture: Future[RequestResult],
	                                                           emptyIsDead: Boolean = false,
	                                                           expectFailure: Boolean = false)
	                                                          (parse: ResponseBody => Try[M])
	                                                          (implicit exc: ExecutionContext) =
	{
		makePointer[M, Try[M]](placeHolder, Success(placeHolder), resultFuture, Flux(!expectFailure)) {
			case Right(body) => parse(body)
			case Left(error) => Failure(error)
		} { (placeHolder, result) => testEmptyState(placeHolder, result, emptyIsDead)(Identity) }
	}
	private[schrodinger] def pullPointer[A](initialManifest: Try[A], placeHolderResult: Try[A],
	                                        resultFuture: Future[RequestResult], parser: FromModelFactory[A],
	                                        flux: Flux = Flux)
	                                       (implicit exc: ExecutionContext) =
	{
		makePointer[Try[A], Try[A]](initialManifest, placeHolderResult, resultFuture, flux) {
			case Right(body) => body.tryParseSingleWith(parser)
			case Left(error) => Failure(error)
		} { (placeHolder, parsed) =>
			val manifest = parsed.orElse { if (placeHolder.isSuccess) placeHolder else parsed }
			(manifest, parsed, Final(parsed.isSuccess))
		}
	}
	private[schrodinger] def makePointer[M, R](initialManifest: M, placeHolderResult: R,
	                                           resultFuture: Future[RequestResult], flux: Flux = Flux)
	                                          (process: Either[Throwable, ResponseBody] => R)
	                                          (merge: (M, R) => (M, R, Final))
	                                          (implicit exc: ExecutionContext) =
	{
		// Stores the state in a Volatile pointer
		val pointer = Volatile[(M, R, SchrodingerState)]((initialManifest, placeHolderResult, flux))
		// Updates the state with future result once it arrives
		resultFuture.onComplete { result =>
			// Parses the item(s) from the response body, if successful
			val parsed = result match {
				case Success(result) =>
					result match {
						case Response.Success(_, body, _) => process(Right(body))
						case failure: RequestFailure => process(Left(failure.cause))
					}
				case Failure(error) => process(Left(error))
			}
			// Updates the pointer
			pointer.update { case (placeHolder, _, _) => merge(placeHolder, parsed) }
		}
		pointer
	}
	
	private[schrodinger] def testEmptyState[L, R <: HasIsEmpty](placeHolder: L, result: Try[R], emptyIsDead: Boolean)
	                                                           (localize: R => L) =
	{
		// Converts an empty result into a failure, if necessary
		val finalResult = {
			if (emptyIsDead)
				result.flatMap { items =>
					if (items.isEmpty)
						Failure(new IllegalStateException("Empty result"))
					else
						Success(items)
				}
			else
				result
		}
		// Returns the manifest, the result and the state
		val manifest = finalResult match {
			case Success(res) => localize(res)
			case Failure(_) => placeHolder
		}
		(manifest, finalResult, Final(finalResult.isSuccess))
	}
}

/**
  * Common trait for Schrödinger items. Schrödinger items may have a flux state where it is still unknown
  * whether the result is a success (alive) or failure (dead) - so in the meanwhile it is a temporary hybrid (flux).
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1 (rewritten 20.11.2022, v1.4)
  * @constructor Creates a new Schrödinger instance that wraps the specified state pointer (abstract)
  * @param fullStatePointer A pointer that contains the current state of this Schrödinger item.
  *                         The state of this item consists of two parts:
  *                             1) The current manifest (placeholder or final), and
  *                             2) The server result that was acquired, if applicable (e.g. as an Option)
  * @tparam M Type of manifest wrapped by this Schrödinger. Manifest is the form in which attempts and results appear
  *           (i.e. the common class between the Schrödinger state, the alive state and the dead state)
  * @tparam R Type for tracking received responses
  */
class Schrodinger2[+M, +R](fullStatePointer: Changing[(M, R, SchrodingerState)]) extends HasSchrodingerState
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * @return A pointer to the current manifest form of the Schrödinger item
	  */
	lazy val manifestPointer: Changing[Manifest[M]] = fullStatePointer.map { case (m, _, state) => Manifest(m, state) }
	/**
	  * @return A pointer to the currently tracked server response / result.
	  */
	lazy val resultPointer: Changing[Manifest[R]] =
		fullStatePointer.map { case (_, r, state) => Manifest(r, state) }
	/**
	  * A pointer to the current state of this schrödinger item, whether it be alive or dead, or flux
	  */
	lazy val statePointer = fullStatePointer.map { _._3 }
	
	/**
	  * @return A future of the eventual server result
	  */
	lazy val finalResultFuture = resultPointer.futureWhere { _.isFinal }
	/**
	  * @return A future that will contain the final state of the tracked Schrödinger
	  *         (based on server result, or irrecoverable lack thereof)
	  */
	lazy val finalManifestFuture: Future[Manifest[M]] = manifestPointer.futureWhere { _.isFinal }
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return The current state of this Schrödinger (Alive (i.e. success), Dead (i.e. failure) or Flux (i.e. pending))
	  */
	def state = fullStatePointer.value._3
	/**
	  * @return Current manifest state of this Schrödinger item.
	  *         This value is based on server result if one has been received,
	  *         otherwise it consists of a temporary placeholder.
	  */
	def manifest = manifestPointer.value
	/**
	  * @return Current result received from the server.
	  */
	def result = resultPointer.value
	
	/**
	  * @return Whether this Schrödinger has been resolved.
	  *         That is, determined alive or dead
	  *         (i.e. whether server result has been received)
	  */
	def isResolved = isFinal
	
	
	// OTHER    ---------------------------
	
	/**
	  * Waits for the instance version that is based on the server result. Wait time can be limited with timeout.
	  * If timeout is reached before server response is received, a temporary instance version is returned instead.
	  * If the server response has already been received, no waiting occurs.
	  * Notice that this method may block for extended time periods, especially when no timeout has been defined.
	  * @param timeout Maximum server result wait duration.
	  *                Default = infinite, which causes this method to block until server result is received.
	  * @param exc Implicit execution context.
	  * @return Instance either based on server result or temporary placeholder, in case the timeout was reached.
	  */
	def waitForManifest(timeout: Duration = Duration.Inf)(implicit exc: ExecutionContext) =
		finalManifestFuture.waitFor(timeout).getOrElse(manifest)
}
