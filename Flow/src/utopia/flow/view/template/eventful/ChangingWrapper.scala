package utopia.flow.view.template.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

object ChangingWrapper
{
	/**
	  * Wraps another changing item
	  * @param c A changing item
	  * @tparam A Type of item's value
	  * @return A new wrapper that views that item
	  */
	def apply[A](c: Changing[A]): Changing[A] = new _ChangingWrapper[A](c)
	
	private class _ChangingWrapper[+A](override val wrapped: Changing[A]) extends ChangingWrapper[A]
}

/**
  * A common trait for views which wrap another changing item
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait ChangingWrapper[+A] extends Changing[A]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return The wrapped changing item
	  */
	protected def wrapped: Changing[A]
	
	
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
	
	override def mergeWith[B, R](other: Changing[B])(f: (A, B) => R) = wrapped.mergeWith(other)(f)
	override def mergeWith[B, C, R](first: Changing[B], second: Changing[C])(merge: (A, B, C) => R) =
		wrapped.mergeWith(first, second)(merge)
	override def lazyMergeWith[B, R](other: Changing[B])(f: (A, B) => R) =
		wrapped.lazyMergeWith(other)(f)
	
	override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext) =
		wrapped.delayedBy(threshold)
	
	override def futureWhere(valueCondition: A => Boolean) = wrapped.futureWhere(valueCondition)
	override def nextFutureWhere(valueCondition: A => Boolean) = wrapped.nextFutureWhere(valueCondition)
	override def existsFixed(condition: A => Boolean) = wrapped.existsFixed(condition)
	override def notFixedWhere(condition: A => Boolean) = wrapped.notFixedWhere(condition)
	override def flatMap[B](f: A => Changing[B]) = wrapped.flatMap(f)
	override def mapAsync[A2 >: A, B, R](placeHolderResult: R, skipInitialMap: Boolean)
	                                    (f: A2 => Future[B])(merge: (R, Try[B]) => R)
	                                    (implicit exc: ExecutionContext) =
		wrapped.mapAsync(placeHolderResult, skipInitialMap)(f)(merge)
	override def tryMapAsync[B](placeHolderResult: B, skipInitialMap: Boolean)
	                           (f: A => Future[Try[B]])(merge: (B, Try[B]) => B)
	                           (implicit exc: ExecutionContext) =
		wrapped.tryMapAsync(placeHolderResult, skipInitialMap)(f)(merge)
	override def mapAsyncCatching[B](placeHolderResult: B, skipInitialMap: Boolean)
	                                (f: A => Future[B])
	                                (implicit exc: ExecutionContext, logger: Logger) =
		wrapped.mapAsyncCatching(placeHolderResult, skipInitialMap)(f)
	override def mapAsyncTryCatching[B](placeHolderResult: B, skipInitialMap: Boolean)
	                                   (f: A => Future[Try[B]])
	                                   (implicit exc: ExecutionContext, logger: Logger) =
		wrapped.mapAsyncTryCatching(placeHolderResult, skipInitialMap)(f)
}
