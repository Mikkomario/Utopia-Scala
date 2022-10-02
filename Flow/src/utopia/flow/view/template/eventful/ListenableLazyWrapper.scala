package utopia.flow.view.template.eventful

import utopia.flow.event.listener.LazyListener
import utopia.flow.view.immutable.eventful.ListenableLazy

/**
  * Common trait for wrappers for listenable lazy containers
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait ListenableLazyWrapper[+A] extends ListenableLazy[A]
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Wrapped listenable lazy instance
	  */
	protected def wrapped: ListenableLazy[A]
	
	
	// IMPLEMENTED  --------------------------
	
	override def current = wrapped.current
	
	override def value = wrapped.value
	
	override def stateView = wrapped.stateView
	
	override def valueFuture = wrapped.valueFuture
	
	override def addListener(listener: => LazyListener[A]) = wrapped.addListener(listener)
	
	override def removeListener(listener: Any) = wrapped.removeListener(listener)
	
	override def map[B](f: A => B): ListenableLazy[B] = wrapped.map(f)
	
	override protected def mapToListenable[B](f: A => B) = wrapped.map(f)
}
