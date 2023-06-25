package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Final, Flux}
import utopia.annex.model.manifest.{HasSchrodingerState, Manifestation, SchrodingerState}
import utopia.annex.model.response.{RequestFailure, RequestResult, Response, ResponseBody}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.event.listener.ChangeListener
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

object Schrodinger
{
	/**
	  * Wraps a pointer into a schrödinger instance
	  * @param pointer A pointer to wrap, which always contains 3 elements:
	  *                 1) Manifest (used during and after the request)
	  *                 2) Result (placeholder until request resolves, then processed request result)
	  *                 3) State (flux, alive or dead)
	  * @tparam M Type of manifest
	  * @tparam R Type of results
	  * @return A new schrödinger
	  */
	def wrap[M, R](pointer: Changing[(M, R, SchrodingerState)]) = new Schrodinger[M, R](pointer)
	
	/**
	  * Creates a new schrödinger that has already resolved into its final state
	  * @param manifest Manifest of the final result
	  * @param result Final result
	  * @param state Alive or Dead, based on whether the result was a success or a failure
	  * @tparam M Type of manifest
	  * @tparam R Type or result
	  * @return A new schrödinger that won't change
	  */
	def resolved[M, R](manifest: M, result: R, state: Final) = wrap(Fixed(manifest, result, state))
	/**
	  * Creates a new schrödinger that has successfully resolved into a final (success) state
	  * @param manifest Manifest of the final result
	  * @param result Final result
	  * @tparam M Type of manifest
	  * @tparam R Type or result
	  * @return A new schrödinger that won't change
	  */
	def success[M, R](manifest: M, result: R) = resolved(manifest, result, Alive)
	/**
	  * Creates a new schrödinger that has resolved into a final failure state
	  * @param manifest Manifest of the final result
	  * @param result   Final result
	  * @tparam M Type of manifest
	  * @tparam R Type or result
	  * @return A new schrödinger that won't change
	  */
	def failure[M, R](manifest: M, result: R) = resolved(manifest, result, Dead)
	
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
class Schrodinger[+M, +R](fullStatePointer: Changing[(M, R, SchrodingerState)]) extends HasSchrodingerState
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * @return A pointer to the current manifest form of the Schrödinger item
	  */
	lazy val manifestPointer: Changing[Manifestation[M]] =
		fullStatePointer.map { case (m, _, state) => Manifestation(m, state) }
	/**
	  * @return A pointer to the currently tracked server response / result.
	  */
	lazy val resultPointer: Changing[Manifestation[R]] =
		fullStatePointer.map { case (_, r, state) => Manifestation(r, state) }
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
	lazy val finalManifestFuture: Future[Manifestation[M]] = manifestPointer.futureWhere { _.isFinal }
	
	
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
	
	/**
	  * Adds a new listener that will be informed whenever the manifestation of this Schrödinger changes
	  * @param simulatedOldValue The manifest that is considered the initial state.
	  *                          Used for creating the immediate change event
	  *                          (provided that the current manifest differs from the specified value)
	  * @param listener A listener to assign to this Schrödinger
	  * @tparam M2 Type of listened manifest value
	  */
	def addManifestListenerAndSimulateEvent[M2 >: M](simulatedOldValue: M2)
	                                                (listener: => ChangeListener[Manifestation[M2]]) =
		manifestPointer.addListenerAndSimulateEvent(
			Manifestation(simulatedOldValue, Some(state).filter { _.isFlux }.getOrElse(Flux)))(listener)
	/**
	  * Adds a new listener that will be informed whenever (i.e. once) the result of this Schrödinger changes
	  * @param simulatedOldValue The placeholder result that is considered the initial state.
	  *                          Used for creating the immediate change event
	  *                          (provided that the currently acquired result or placeholder
	  *                          differs from the specified value)
	  * @param listener          A listener to assign to this Schrödinger
	  * @tparam R2 Type of listened result value
	  */
	def addResultListenerAndSimulateEvent[R2 >: R](simulatedOldValue: R2)(listener: ChangeListener[Manifestation[R2]]) =
		resultPointer.addListenerAndSimulateEvent(
			Manifestation(simulatedOldValue, Some(state).filter { _.isFlux }.getOrElse(Flux)))(listener)
}
