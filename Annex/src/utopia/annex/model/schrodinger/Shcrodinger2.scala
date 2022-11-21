package utopia.annex.model.schrodinger

import utopia.flow.async.AsyncExtensions._
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Common trait for Schrödinger items. Schrödinger items may have a flux state where it is still unknown
  * whether the result is a success (alive) or failure (dead) - so in the meanwhile it is a temporary hybrid.
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1 (rewritten 20.11.2022, v1.4)
  * @constructor Creates a new Schrödinger instance that wraps the specified state pointer (abstract)
  * @param fullStatePointer A pointer that contains the current state of this Schrödinger item.
  *                     The state of this item consists of two parts:
  *                         1) The current manifest (placeholder or final), and
  *                         2) The server result that was acquired, if applicable (i.e. as an Option)
  * @tparam M Type of manifest wrapped by this Schrödinger. Manifest is the form in which attempts and results appear
  *           (i.e. the common class between the Schrödinger state, the alive state and the dead state)
  * @tparam R Type of received response
  */
abstract class Shcrodinger2[+M, +R](fullStatePointer: Changing[(M, R, SchrodingerState)]) extends HasSchrodingerState
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * @return A pointer to the current manifest form of the Schrödinger item
	  */
	lazy val manifestPointer: Changing[Manifest[M]] = fullStatePointer.map { case (m, _, state) => Manifest(m, state) }
	/**
	  * @return A pointer to the currently tracked server response / result.
	  *         Contains None while the server response hasn't been received or resolved and
	  *         Some after the final state has been acquired.
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
	  *         None until results have actually been received.
	  */
	def result = resultPointer.value
	
	/**
	  * @return Whether this shcrödinger has been resolved. That is, determined alive or dead (server result has been
	  *         received)
	  */
	def isResolved = isFinal
	
	
	// OTHER    ---------------------------
	
	/**
	  * Waits for the instance version that is based on the server result. Wait time can be limited with timeout. If
	  * timeout is reached before server response is received, a temporary instance version is generated instead.
	  * If the server response has already been received, no waiting occurs. Notice that this method may block for
	  * extended time periods, especially when no timeout has been defined.
	  * @param timeout Maximum server result wait duration. Default = infinite, which causes this method to block until
	  *                server result is received.
	  * @param exc Implicit execution context.
	  * @return Instance either based on server result or temporary placeholder, in case timeout was reached.
	  */
	def waitForManifest(timeout: Duration = Duration.Inf)(implicit exc: ExecutionContext) =
		finalManifestFuture.waitFor(timeout).getOrElse(manifest)
}
