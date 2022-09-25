package utopia.annex.model.schrodinger

import utopia.flow.async.AsyncExtensions._
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

/**
  * Common trait for Schrödinger items. Schrödinger items may have a flux state where it is still unknown
  * whether the result is a success (alive) or failure (dead) - so in the meanwhile it is a temporary hybrid.
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  * @tparam R Type of received response
  * @tparam I Type of instance within response
  */
trait ShcrodingerLike[+R, +I]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return A pointer to the currently tracked instance value
	  */
	def instancePointer: Changing[I]
	
	/**
	  * @param exc Implicit execution context
	  * @return A future of the eventual server result (success or failure)
	  */
	def serverResultFuture(implicit exc: ExecutionContext): Future[R]
	
	/**
	  * @return A future that will contain the final state of the tracked instance (based on server result)
	  */
	def finalInstanceFuture(implicit exc: ExecutionContext): Future[I]
	
	/**
	  * @return A pointer to the currently tracked server response. Contains None while the server response hasn't been
	  *         received or resolved.
	  */
	def serverResultPointer: Changing[Option[R]]
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Whether this shcrödinger has been resolved. That is, determined alive or dead (server result has been
	  *         received)
	  */
	def isResolved = serverResult.isDefined
	
	/**
	  * @return Current instance state of this schrödinger item. Is based on server result if one has been received,
	  *         otherwise returns a temporary placeholder.
	  */
	def instance = instancePointer.value
	
	/**
	  * @return Current result received from the server, if any result has been received yet
	  */
	def serverResult = serverResultPointer.value
	
	
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
	def waitForInstance(timeout: Duration = Duration.Inf)(implicit exc: ExecutionContext) =
		finalInstanceFuture.waitFor(timeout).getOrElse(instance)
}
