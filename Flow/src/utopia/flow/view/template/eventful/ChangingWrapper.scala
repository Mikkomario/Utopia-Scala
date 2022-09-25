package utopia.flow.view.template.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object ChangingWrapper
{
	/**
	  * Wraps another changing item
	  * @param c A changing item
	  * @tparam A Type of item's value
	  * @return A new wrapper that views that item
	  */
	def apply[A](c: ChangingLike[A]): ChangingLike[A] = new _ChangingWrapper[A](c)
	
	private class _ChangingWrapper[+A](override val wrapped: ChangingLike[A]) extends ChangingWrapper[A]
}

/**
  * A common trait for views which wrap another changing item
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait ChangingWrapper[+A] extends ChangingLike[A]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return The wrapped changing item
	  */
	protected def wrapped: ChangingLike[A]
	
	
	// IMPLEMENTED  --------------------
	
	override def value = wrapped.value
	
	override def isChanging = wrapped.isChanging
	
	override def addListener(changeListener: => ChangeListener[A]) = wrapped.addListener(changeListener)
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
		wrapped.addListenerAndSimulateEvent(simulatedOldValue)(changeListener)
	
	override def removeListener(changeListener: Any) = wrapped.removeListener(changeListener)
	
	override def addDependency(dependency: => ChangeDependency[A]) = wrapped.addDependency(dependency)
	override def removeDependency(dependency: Any) = wrapped.removeDependency(dependency)
	
	override def map[B](f: A => B) = wrapped.map(f)
	override def lazyMap[B](f: A => B) = wrapped.lazyMap(f)
	
	override def mergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) = wrapped.mergeWith(other)(f)
	override def mergeWith[B, C, R](first: ChangingLike[B], second: ChangingLike[C])(merge: (A, B, C) => R) =
		wrapped.mergeWith(first, second)(merge)
	override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
		wrapped.lazyMergeWith(other)(f)
	
	override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext) =
		wrapped.delayedBy(threshold)
}
