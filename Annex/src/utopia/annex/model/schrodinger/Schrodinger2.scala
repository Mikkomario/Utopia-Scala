package utopia.annex.model.schrodinger

import utopia.annex.model.response.ResponseBody.Content
import utopia.annex.model.response.{RequestFailure, RequestResult, Response, ResponseBody}
import utopia.annex.model.schrodinger.SchrodingerState.{Final, Flux}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

object Schrodinger2
{
	def wrap[M, R](pointer: Changing[(M, R, SchrodingerState)]) = new Schrodinger2[M, R](pointer)
	
	/**
	  * Creates a schrodinger that contains 0-n locally cached values until server results arrive,
	  * from which are parsed a new set of items, if possible.
	  * Typically used when updating locally cached data.
	  * @param local A result based on local data pull (0-n items as a Vector)
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser A parser used for parsing items from a server response
	  * @param emptyIsDead Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                    Default = false = Result is alive as long as it resolves successfully.
	  * @param exc Implicit execution context
	  * @tparam A Type of individual pulled items
	  * @return A new schrödinger that contains local pull results
	  *         and updates itself once the server-side results arrive.
	  */
	def pullMany[A](local: Vector[A], resultFuture: Future[RequestResult], parser: FromModelFactory[A],
	                emptyIsDead: Boolean = false)
	               (implicit exc: ExecutionContext) =
		_get(local, Vector(), resultFuture, emptyIsDead) { _.vector(parser).parsed }
	/**
	  * Creates a schrodinger that contains 0-1 locally cached values until server results arrive,
	  * from which a new item is parsed, if present and possible.
	  * Typically used when updating locally cached data.
	  * @param local        A result based on local data pull (0-n items as a Vector)
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser       A parser used for parsing items from a server response
	  * @param emptyIsDead  Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                     Default = false = Result is alive as long as it resolves successfully.
	  * @param exc          Implicit execution context
	  * @tparam A Type of individual pulled items
	  * @return A new schrödinger that contains local pull results
	  *         and updates itself once the server-side results arrive.
	  */
	def find[A](local: Option[A], resultFuture: Future[RequestResult], parser: FromModelFactory[A],
	            emptyIsDead: Boolean = false)
	           (implicit exc: ExecutionContext) =
		_get(local, None, resultFuture, emptyIsDead) {
			case c: Content => c.parsedSingle(parser).map { Some(_) }
			case _ => Success(None)
		}
	/**
	  * Pulls a local and a remote instance.
	  * Stores the local instance (if available) until a remote instance is acquired.
	  * Expects a non-empty response from the server.
	  * Typically used in situations where locally cached data may act as a placeholder until server results arrive.
	  * @param local Locally cached data. None if no local data is available (which is an impermanent failure case).
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser A parser used for parsing items from a server response
	  * @param exc Implicit execution context
	  * @tparam A Type of found items, when successful
	  * @return A new schrödinger that contains local data as a placeholder until server results arrive.
	  *         Dead if no item could be read from the server response
	  *         (whether the request itself was successful or not)
	  */
	def pull[A](local: Option[A], resultFuture: Future[RequestResult], parser: FromModelFactory[A])
	           (implicit exc: ExecutionContext) =
	{
		_apply[Try[A], Try[A]](local.toTry { new NoSuchElementException("No local data") },
			Failure(new IllegalStateException("Remote result is still pending")), resultFuture, Flux(local.isDefined)) {
			case Right(body) => body.tryParseSingleWith(parser)
			case Left(error) => Failure(error)
		} { (placeHolder, parsed) =>
			parsed.orElse { if (placeHolder.isSuccess) placeHolder else parsed } -> Final(parsed.isSuccess)
		}
	}
	
	private def _get[M <: { def isEmpty: Boolean }](local: M, placeHolder: M, resultFuture: Future[RequestResult],
	                                                emptyIsDead: Boolean = false)
	                                               (parse: ResponseBody => Try[M])
	                                               (implicit exc: ExecutionContext) =
	{
		_apply[M, Try[M]](local, Success(placeHolder), resultFuture, Flux(!emptyIsDead || !local.isEmpty)) {
			case Right(body) => parse(body)
			case Left(error) => Failure(error)
		} { (placeHolder, parsed) =>
			val state = {
				if (emptyIsDead)
					parsed.toOption.exists { !_.isEmpty }
				else
					parsed.isSuccess
			}
			parsed.getOrElse(placeHolder) -> Final(state)
		}
	}
	private def _apply[M, R](local: M, placeHolder: R, resultFuture: Future[RequestResult], flux: Flux = Flux)
	                        (process: Either[Throwable, ResponseBody] => R)(merge: (M, R) => (M, Final))
	           (implicit exc: ExecutionContext) =
	{
		// Stores the state in a Volatile pointer
		val pointer = Volatile[(M, R, SchrodingerState)]((local, placeHolder, flux))
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
			pointer.update { case (placeHolder, _, _) =>
				val (manifest, state) = merge(placeHolder, parsed)
				(manifest, parsed, state)
			}
		}
		wrap(pointer)
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
