package utopia.flow.event

import utopia.flow.async.DelayedView
import utopia.flow.datastructure.immutable.Lazy

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

object Changing
{
	// OTHER	---------------------
	
	/**
	  * Wraps an immutable value as a "changing" instance that won't actually change
	  * @param value A value being wrapped
	  * @tparam A Type of wrapped value
	  * @return An unchanging instance that will always have the specified value
	  */
	@deprecated("Please use Fixed.apply(...) instead", "v1.9")
	def wrap[A](value: A): ChangingLike[A] = Fixed(value)
}

/**
  * Changing instances generate change events
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1
  */
trait Changing[A] extends ChangingLike[A]
{
	// ABSTRACT	---------------------
	
	/**
	 * @return Listeners linked to this changing instance
	 */
	def listeners: Vector[ChangeListener[A]]
	
	/**
	 * Updates this instances listeners
	 * @param newListeners New listeners to associate with this changing instance
	 */
	def listeners_=(newListeners: Vector[ChangeListener[A]]): Unit
	
	
	// IMPLEMENTED	----------------
	
	override def addListener(changeListener: => ChangeListener[A]) = listeners :+= changeListener
	
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
	{
		val newListener = changeListener
		listeners :+= newListener
		simulateChangeEventFor[B](newListener, simulatedOldValue)
	}
	
	override def removeListener(listener: Any) = listeners = listeners.filterNot { _ == listener }
	
	override def futureWhere(valueCondition: A => Boolean)(implicit exc: ExecutionContext) =
		defaultFutureWhere(valueCondition)
	
	override def map[B](f: A => B) = if (isChanging) Mirror(this)(f) else Fixed(f(value))
	
	override def lazyMap[B](f: A => B) = if (isChanging) LazyMirror(this)(f) else Lazy { f(value) }
	
	override def mergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
	{
		if (other.isChanging)
		{
			if (isChanging)
				MergeMirror(this, other)(f)
			else
				other.map { f(value, _) }
		}
		else
			map { f(_, other.value) }
	}
	
	override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
	{
		if (other.isChanging)
		{
			if (isChanging)
				LazyMergeMirror(this, other)(f)
			else
				other.lazyMap { f(value, _) }
		}
		else
			lazyMap { f(_, other.value) }
	}
	
	override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext) =
		DelayedView.of(this, threshold)
	
	
	// OTHER	--------------------
	
	/**
	  * Fires a change event for all the listeners
	  * @param oldValue The old value of this changing element (call-by-name)
	  */
	protected def fireChangeEvent(oldValue: => A) =
	{
		lazy val event = ChangeEvent(oldValue, value)
		listeners.foreach { _.onChangeEvent(event) }
	}
}