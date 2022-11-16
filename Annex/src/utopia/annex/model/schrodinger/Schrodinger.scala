package utopia.annex.model.schrodinger

import utopia.flow.view.immutable.caching.LazyFuture
import utopia.flow.view.mutable.eventful.PointerWithEvents

import scala.concurrent.ExecutionContext

/**
  * Common parent class for Schrödinger items. Schrödinger items may have a flux state where it is still unknown
  * whether the result is a success (alive) or failure (dead) - so in the meanwhile it is a temporary hybrid.
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  * @tparam R Type of received response
  * @tparam I Type of instance within response
  */
trait Schrodinger[R, +I] extends ShcrodingerLike[R, I]
{
	// ABSTRACT ---------------------------
	
	/**
	  * Parses replicate or actual instance from the result, or the lack of it
	  * @param result Result received from server. None if no result has been received yet.
	  * @return An instance that is suitable in the current situation.
	  */
	protected def instanceFrom(result: Option[R]): I
	
	
	// ATTRIBUTES   -----------------------
	
	protected val _serverResultPointer: PointerWithEvents[Option[R]] = new PointerWithEvents(None)
	
	/**
	  * A pointer to the currently mimicked instance
	  */
	lazy val instancePointer = serverResultPointer.map(instanceFrom)
	
	private val _serverResultFuture = LazyFuture.flatten { _ => serverResultPointer.findMapFuture { r => r } }
	
	
	// COMPUTED ---------------------------
	
	override def serverResultPointer = _serverResultPointer
	
	private def serverResult_=(newResult: R) = _serverResultPointer.value = Some(newResult)
	
	/**
	  * @return A read-only view into this schrödinger
	  */
	def view = new SchrodingerView(this)
	
	
	// IMPLEMENTED	-----------------------
	
	override def serverResultFuture(implicit exc: ExecutionContext) = _serverResultFuture.value
	
	override def finalInstanceFuture(implicit exc: ExecutionContext) = serverResultFuture.map { r =>
		instanceFrom(Some(r)) }
	
	
	// OTHER    ---------------------------
	
	/**
	  * Completes this schrödinger item with a server result
	  * @param result Received result
	  */
	def complete(result: R) = serverResult = result
}
