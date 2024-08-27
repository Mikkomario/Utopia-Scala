package utopia.flow.view.template.eventful

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.operator.enumeration.End
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.EventfulPointer

/**
  * Common abstract class for pointer that behave differently while they are being listened to,
  * optimize their behavior accordingly.
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
abstract class OptimizedChanging[A]
	extends ChangingWithListeners[A] with MayStopChanging[A]
{
	// ATTRIBUTES   -------------------------
	
	// Stores the listeners in a pointer, because this mirror functions differently while there are listeners assigned
	private val listenersPointer = EventfulPointer[Pair[Seq[ChangeListener[A]]]](Pair.twice(Empty))
	/**
	  * A pointer that contains true while this pointer has listeners attached
	  */
	val hasListenersFlag: FlagLike = listenersPointer.strongMap { _.exists { _.nonEmpty } }
	
	private var stopListeners: Seq[ChangingStoppedListener] = Empty
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasListeners = hasListenersFlag.value
	
	override protected def listenersByPriority: Pair[Iterable[ChangeListener[A]]] = listenersPointer.value
	
	override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit =
		listenersPointer.update {
			_.mapSide(priority) { q => if (q.contains(lazyListener.value)) q else q :+ lazyListener.value }
		}
	override def removeListener(changeListener: Any): Unit =
		listenersPointer.update { _.map { _.filterNot { _ == changeListener } } }
	override protected def removeListeners(priority: End, listenersToRemove: Iterable[ChangeListener[A]]): Unit =
		listenersPointer.update { _.mapSide(priority) { _.filterNot { l => listenersToRemove.exists { _ == l } } } }
	
	override protected def declareChangingStopped(): Unit = {
		clearListeners()
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
	def clearListeners() = {
		if (hasListeners)
			listenersPointer.value = Pair.twice(Empty)
	}
}
