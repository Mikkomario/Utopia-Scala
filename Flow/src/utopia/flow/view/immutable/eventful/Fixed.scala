package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.operator.enumeration.End
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.View
import utopia.flow.view.template.eventful.{Changing, FlagLike}

object Fixed
{
	/**
	  * A pointer/view that never contains any value
	  */
	val never = apply(None)
}
case class Fixed[+A](override val value: A) extends Changing[A]
{
	// IMPLEMENTED	-------------
	
	override implicit def listenerLogger: Logger = SysErrLogger
	
	override def destiny: Destiny = Sealed
	
	override def hasListeners: Boolean = false
	override def numberOfListeners: Int = 0
	
	override def readOnly = this
	
	override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit = ()
	override def removeListener(changeListener: Any) = ()
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	
	override def mergeWith[B, R](other: Changing[B])(f: (A, B) => R) = other.map { f(value, _) }
}

/**
  * A pointer that always contains 'true'
  */
object AlwaysTrue extends Fixed(true) with FlagLike
{
	override def unary_! = AlwaysFalse
	
	override def &&(other: Changing[Boolean]) = other
	override def ||(other: Changing[Boolean]) = this
}
/**
  * A pointer that always contains 'false'
  */
object AlwaysFalse extends Fixed(false) with FlagLike
{
	override def unary_! = AlwaysTrue
	
	override def &&(other: Changing[Boolean]) = this
	override def ||(other: Changing[Boolean]) = other
}