package utopia.flow.event

import utopia.flow.async.DelayedView
import utopia.flow.collection.template.Viewable
import utopia.flow.collection.template.caching.ListenableLazyLike
import utopia.flow.collection.value.View

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
	 * Updates this instance's listeners
	 * @param newListeners New listeners to associate with this changing instance
	 */
	def listeners_=(newListeners: Vector[ChangeListener[A]]): Unit
	
	/**
	  * @return Dependencies linked to this changing instance
	  */
	def dependencies: Vector[ChangeDependency[A]]
	/**
	  * Updates this instance's dependencies
	  * @param newDependencies Dependencies to apply to this instance
	  */
	def dependencies_=(newDependencies: Vector[ChangeDependency[A]]): Unit
	
	
	// IMPLEMENTED	----------------
	
	override def addListener(changeListener: => ChangeListener[A]) = listeners :+= changeListener
	
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
	{
		val newListener = changeListener
		if (simulateChangeEventFor[B](newListener, simulatedOldValue).shouldContinue)
			listeners :+= newListener
	}
	
	override def removeListener(listener: Any) = listeners = listeners.filterNot { _ == listener }
	
	override def addDependency(dependency: => ChangeDependency[A]) = dependencies :+= dependency
	override def removeDependency(dependency: Any) = dependencies = dependencies.filterNot { _ == dependency }
	
	override def map[B](f: A => B) = Mirror.of(this)(f)
	
	override def lazyMap[B](f: A => B): ListenableLazyLike[B] = LazyMirror.of(this)(f)
	
	override def mergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
	{
		if (other.isChanging)
		{
			if (isChanging)
				new MergeMirror(this, other)(f)
			else
				other.map { f(value, _) }
		}
		else
			map { f(_, other.value) }
	}
	
	override def mergeWith[B, C, R](first: ChangingLike[B], second: ChangingLike[C])
	                               (merge: (A, B, C) => R): ChangingLike[R] =
		TripleMergeMirror.of(this, first, second)(merge)
	
	override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R): ListenableLazyLike[R] =
	{
		if (other.isChanging)
		{
			if (isChanging)
				new LazyMergeMirror(this, other)(f)
			else
				other.lazyMap { f(value, _) }
		}
		else
			lazyMap { f(_, other.value) }
	}
	
	override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext): ChangingLike[A] =
		DelayedView.of(this, threshold)
	
	
	// OTHER	--------------------
	
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param oldValue The old value of this changing element (call-by-name)
	  */
	protected def fireChangeEvent(oldValue: => A) = _fireEvent(Lazy { ChangeEvent(oldValue, value) })
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param event A change event to fire (should be lazily initialized)
	  */
	protected def fireEvent(event: ChangeEvent[A]) = _fireEvent(View(event))
	private def _fireEvent(event: Viewable[ChangeEvent[A]]) = {
		// Informs the dependencies first
		val afterEffects = dependencies.flatMap { _.beforeChangeEvent(event.value) }
		// Then the listeners (may remove some listeners in the process)
		listeners = listeners.filter { _.onChangeEvent(event.value).shouldContinue }
		// Finally performs the after-effects defined by the dependencies
		afterEffects.foreach { _() }
	}
	
	/**
	  * Starts mirroring another pointer
	  * @param origin Origin value pointer
	  * @param map A value transformation function that accepts the new origin pointer value
	  * @param set A function for updating this pointer's value
	  * @tparam O Type of origin pointer's value
	  */
	protected def startMirroring[O](origin: ChangingLike[O])(map: O => A)(set: A => Unit) =
	{
		// Registers as a dependency for the origin pointer
		origin.addDependency(ChangeDependency { e1: ChangeEvent[O] =>
			// Whenever the origin's value changes, calculates a new value
			val newValue = map(e1.newValue)
			val oldValue = value
			// If the new value is different from the previous state, updates the value and generates a new change event
			if (newValue != oldValue)
			{
				set(newValue)
				val event2 = ChangeEvent(oldValue, newValue)
				// The dependencies are informed immediately, other listeners only afterwards
				val afterEffects = dependencies.flatMap { _.beforeChangeEvent(event2) }
				if (afterEffects.nonEmpty || listeners.nonEmpty)
					Some(event2 -> afterEffects)
				else
					None
			}
			else
				None
		} { case (event, actions) =>
			// After the origin has finished its update, informs the listeners and triggers the dependency after-effects
			listeners.foreach { _.onChangeEvent(event) }
			actions.foreach { _() }
		})
	}
}