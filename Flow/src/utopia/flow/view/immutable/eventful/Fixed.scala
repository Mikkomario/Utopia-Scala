package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Single
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.{Lazy, LazyWrapper}
import utopia.flow.view.template.eventful.{Changing, Flag}

object Fixed
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * A pointer/view that never contains any value
	  */
	val never = apply(None)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param value Value to wrap
	  * @tparam A Type of the wrapped value
	  * @return A fixed pointer forever wrapping that value
	  */
	def apply[A](value: A): Fixed[A] = new _Fixed[A](value)
	/**
	  * @param value Value to wrap (call-by-name)
	  * @tparam A Type of the wrapped value
	  * @return A fixed pointer that wraps the specified value, but initializes itself lazily
	  */
	def lazily[A](value: => A): LazilyFixed[A] = LazilyFixed[A](value)
	
	
	// NESTED   ------------------------
	
	private class _Fixed[+A](override val value: A) extends Fixed[A]
}
/**
  * Common class for pointers that never change
  * @tparam A Type of the wrapped fixed value
  */
abstract class Fixed[+A] extends Changing[A] with EqualsBy
{
	// ATTRIBUTES   -------------
	
	override val listenerLogger: Logger = SysErrLogger
	override val destiny: Destiny = Sealed
	override val hasListeners: Boolean = false
	override val numberOfListeners: Int = 0
	
	
	// IMPLEMENTED	-------------
	
	override def readOnly: Changing[A] = this
	override protected def equalsProperties: Seq[Any] = Single(value)
	
	override def toString = s"Always($value)"
	
	override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit = ()
	override def removeListener(changeListener: Any) = ()
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	
	// Synchronization is not required since this pointer is fixed
	override def lockWhile[B](operation: => B): B = operation
	
	override def mergeWith[B, R](other: Changing[B])(f: (A, B) => R) = other.map { f(value, _) }
}

object LazilyFixed
{
	/**
	  * @param get A function that yields the lazily acquired value
	  * @tparam A Type of value to acquire
	  * @return A lazily initialized fixed pointer
	  */
	def apply[A](get: => A) = new LazilyFixed[A](Lazy(get))
}
/**
  * A pointer that initializes its value lazily, and never changes afterwards
  * @param wrapped The wrapped lazy container
  * @tparam A Type of the wrapped fixed value
  */
sealed case class LazilyFixed[+A] private(wrapped: Lazy[A]) extends Fixed[A] with LazyWrapper[A]
{
	// IMPLEMENTED  --------------------
	
	override def toString = current match {
		case Some(value) => s"Always.lazily($value)"
		case None => "Always.lazily(<unitialized>)"
	}
	
	override def map[B](f: A => B) = new LazilyFixed[B](wrapped.map(f))
	override def lightMap[B](f: A => B) = new LazilyFixed[B](wrapped.lightMap(f))
}

/**
  * A pointer that always contains 'true'
  */
object AlwaysTrue extends Fixed[Boolean] with Flag
{
	override val value: Boolean = true
	
	override def unary_! = AlwaysFalse
	
	override def &&(other: Flag) = other
	override def ||(other: Flag) = this
}
/**
  * A pointer that always contains 'false'
  */
object AlwaysFalse extends Fixed[Boolean] with Flag
{
	override val value: Boolean = false
	
	override def unary_! = AlwaysTrue
	
	override def &&(other: Flag) = this
	override def ||(other: Flag) = other
}