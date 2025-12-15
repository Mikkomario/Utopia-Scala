package utopia.flow.view.template.eventful

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.ChangeResponsePriority
import utopia.flow.event.model.ChangeResponsePriority.High
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.async.Volatile

/**
  * Common abstract class for pointer that behave differently while they are being listened to,
  * optimize their behavior accordingly.
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
abstract class OptimizedChanging[A] extends ChangingWithListeners[A] with MayStopChanging[A]
{
	// ATTRIBUTES   -------------------------
	
	// TODO: Could wrap these in an optional pointer, and clear the value when changing stops
	private val listenerPointers = ChangeResponsePriority.descending.iterator
		.map { _ -> Volatile.lockable.emptySeq[ChangeListener[A]] }.toMap
	private val _hasListenersFlag = {
		// TODO: Refactor this flag, so that the change events are generated
		//  as after-effects in the applicable listener pointer
		val nonEmptyPointers = listenerPointers.valuesIterator.map { _.strongMap { _.nonEmpty } }.toOptimizedSeq
		val nonEmptyFlag = Flag.resettable.lockable()
		val updateFlagListener = ChangeListener.onAnyChange { nonEmptyFlag.value = nonEmptyPointers.exists { _.value } }
		nonEmptyPointers.foreach { _.addListenerOfPriority(High)(updateFlagListener) }
		
		nonEmptyFlag
	}
	
	private var stopListeners: Seq[ChangingStoppedListener] = Empty
	
	
	// COMPUTED -----------------------------
	
	/**
	 * A pointer that contains true while this pointer has listeners attached
	 */
	def hasListenersFlag = _hasListenersFlag.view
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasListeners = hasListenersFlag.value
	
	override protected def listenersOfPriority(priority: ChangeResponsePriority): IterableOnce[ChangeListener[A]] =
		listenerPointers(priority).value
	
	override def removeListener(changeListener: Any): Unit =
		ChangeResponsePriority.ascendingIterator.find { prio =>
			val p = listenerPointers(prio)
			p.locked || p.mutate { _.findAndPop { _ == changeListener } }.isDefined
		}
	override protected def removeListener(priority: ChangeResponsePriority, listenerToRemove: ChangeListener[A]): Unit =
		listenerPointers(priority).tryUpdate { _.filterNot { _ == listenerToRemove } }
	override protected def _addListenerOfPriority(priority: ChangeResponsePriority, lazyListener: View[ChangeListener[A]]): Unit = {
		val listener = lazyListener.value
		listenerPointers(priority).tryUpdate { listeners =>
			if (listeners.contains(listener))
				listeners
			else
				listeners :+ listener
		}
	}
	
	override protected def declareChangingStopped(): Unit = {
		listenerPointers.valuesIterator.foreach { p =>
			if (p.unlocked) {
				p.clear()
				p.lock()
			}
		}
		_hasListenersFlag.lock()
		if (stopListeners.nonEmpty) {
			stopListeners.foreach { _.onChangingStopped() }
			stopListeners = Empty
		}
	}
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit =
		stopListeners :+= listener
	
	
	// OTHER    ----------------------------
	
	/**
	  * Removes all listeners that have been assigned to this pointer
	  */
	// TODO: Look whether this may be made protected
	def clearListeners() = {
		if (hasListeners)
			listenerPointers.valuesIterator.foreach { _.trySet(Empty) }
	}
}
