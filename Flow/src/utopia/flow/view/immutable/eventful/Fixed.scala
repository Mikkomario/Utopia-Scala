package utopia.flow.view.immutable.eventful

import utopia.flow.async.context.SynchronousExecutionContext
import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.eventful.ChangingLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

case class Fixed[+A](override val value: A) extends ChangingLike[A]
{
	// IMPLEMENTED	-------------
	
	override def isChanging = false
	
	override def removeListener(changeListener: Any) = ()
	override def removeDependency(dependency: Any) = ()
	
	override def mergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
		other.map { f(value, _) }
	
	override def mergeWith[B, C, R](first: ChangingLike[B], second: ChangingLike[C])
	                               (merge: (A, B, C) => R): ChangingLike[R] =
		first.mergeWith(second) { (b, c) => merge(value, b, c) }
	
	override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
		other.lazyMap { f(value, _) }
	
	override def addListener(changeListener: => ChangeListener[A]) = ()
	
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
		simulateChangeEventFor(changeListener, simulatedOldValue)
	
	override def addDependency(dependency: => ChangeDependency[A]) = ()
	
	override def map[B](f: A => B) = Fixed(f(value))
	
	override def lazyMap[B](f: A => B) = Lazy.listenable { f(value) }
	
	override def delayedBy(threshold: Duration)
	                      (implicit exc: ExecutionContext = SynchronousExecutionContext) =
		this
}

/**
  * A pointer that always contains 'true'
  */
object AlwaysTrue extends Fixed(true)

/**
  * A pointer that always contains 'false'
  */
object AlwaysFalse extends Fixed(false)