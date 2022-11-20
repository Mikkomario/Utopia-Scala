package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.view.template.eventful.{Changing, FlagLike}

case class Fixed[+A](override val value: A) extends Changing[A]
{
	// IMPLEMENTED	-------------
	
	override def isChanging = false
	
	override def removeListener(changeListener: Any) = ()
	override def removeDependency(dependency: Any) = ()
	
	override def mergeWith[B, R](other: Changing[B])(f: (A, B) => R) = other.map { f(value, _) }
	
	override def addListener(changeListener: => ChangeListener[A]) = ()
	
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
		simulateChangeEventFor(changeListener, simulatedOldValue)
	
	override def addDependency(dependency: => ChangeDependency[A]) = ()
}

/**
  * A pointer that always contains 'true'
  */
object AlwaysTrue extends Fixed(true) with FlagLike
/**
  * A pointer that always contains 'false'
  */
object AlwaysFalse extends Fixed(false) with FlagLike