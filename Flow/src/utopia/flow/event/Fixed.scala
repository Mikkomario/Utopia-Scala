package utopia.flow.event

import utopia.flow.datastructure.immutable.Lazy

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

case class Fixed[+A](override val value: A) extends ChangingLike[A]
{
	// IMPLEMENTED	-------------
	
	override def isChanging = false
	
	override def removeListener(changeListener: Any) = ()
	
	override def mergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
		other.map { f(value, _) }
	
	override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
		other.lazyMap { f(value, _) }
	
	override def addListener(changeListener: => ChangeListener[A]) = ()
	
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
		simulateChangeEventFor(changeListener, simulatedOldValue)
	
	override def futureWhere(valueCondition: A => Boolean)(implicit exc: ExecutionContext) =
	{
		// Will return either a completed future or a future that never completes
		if (valueCondition(value))
			Future.successful(value)
		else
			Future.never
	}
	
	override def map[B](f: A => B) = Fixed(f(value))
	
	override def lazyMap[B](f: A => B) = Lazy { f(value) }
	
	override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext) = this
}



