package utopia.annex.model.schrodinger

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.async.AsyncExtensions._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * Common parent class for Schrödinger items. Schrödinger items may have a flux state where it is still unknown
  * whether the result is a success (alive) or failure (dead) - so in the meanwhile it is a temporary hybrid.
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait Schrodinger[R, I]
{
	// ABSTRACT ---------------------------
	
	/**
	  * Parses replicate or actual instance from the result, or the lack of it
	  * @param result Result received from server. None if no result has been received yet.
	  * @return An instance that is suitable in the current situation.
	  */
	protected def instanceFrom(result: Option[R]): I
	
	
	// ATTRIBUTES   -----------------------
	
	private val serverResultPointer: PointerWithEvents[Option[R]] = new PointerWithEvents(None)
	
	/**
	  * A pointer to the currently mimicked instance
	  */
	lazy val instancePointer = serverResultPointer.map(instanceFrom)
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Current instance state of this schrödinger item. Is based on server result if one has been received,
	  *         otherwise returns a temporary placeholder.
	  */
	def instance = instancePointer.value
	
	/**
	  * @return Current result received from the server, if any result has been received yet
	  */
	def serverResult = serverResultPointer.value
	private def serverResult_=(newResult: R) = serverResultPointer.value = Some(newResult)
	
	/**
	  * @param exc Implicit execution context
	  * @return A future of the eventual server result (success or failure)
	  */
	def serverResultFuture(implicit exc: ExecutionContext) =
		serverResultPointer.futureWhere { _.isDefined }.map { _.get }
	
	
	// OTHER    ---------------------------
	
	/**
	  * Completes this schrödinger item with a server result
	  * @param result Received result
	  */
	def complete(result: R) = serverResult = result
	
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
	def waitForInstance(timeout: Duration = Duration.Inf)(implicit exc: ExecutionContext) =
		instanceFrom(serverResultPointer.futureWhere { _.isDefined }.waitFor(timeout).getOrElse(None))
}
