package utopia.flow.collection.template.caching

import utopia.flow.event.LazyListener

/**
  * Common trait for wrappers for listenable lazy containers
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait ListenableLazyWrapper[+A] extends ListenableLazyLike[A]
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Wrapped listenable lazy instance
	  */
	protected def wrapped: ListenableLazyLike[A]
	
	
	// IMPLEMENTED  --------------------------
	
	override def current = wrapped.current
	
	override def value = wrapped.value
	
	override def stateView = wrapped.stateView
	
	override def valueFuture = wrapped.valueFuture
	
	override def addListener(listener: => LazyListener[A]) = wrapped.addListener(listener)
	
	override def removeListener(listener: Any) = wrapped.removeListener(listener)
	
	override def map[B](f: A => B) = wrapped.map(f)
}
