package utopia.flow.view.template.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.End
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.EventfulPointer

/**
  * Common abstract class for pointer that behave differently while they are being listened to,
  * optimize their behavior accordingly.
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
abstract class OptimizedChanging[A] extends ChangingWithListeners[A]
{
	// ATTRIBUTES   -------------------------
	
	// Stores the listeners in a pointer, because this mirror functions differently while there are listeners assigned
	private val listenersPointer = new EventfulPointer[Pair[Vector[ChangeListener[A]]]](Pair.twice(Vector.empty))
	/**
	  * A pointer that contains true while this pointer has listeners attached
	  */
	protected val hasListenersFlag = listenersPointer.strongMap { _.exists { _.nonEmpty } }
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasListeners = hasListenersFlag.value
	
	override protected def listenersByPriority: Pair[Iterable[ChangeListener[A]]] = listenersPointer.value
	
	override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit =
		listenersPointer.update {
			_.mapSide(priority) { q => if (q.contains(lazyListener.value)) q else q :+ lazyListener.value }
		}
	override def removeListener(changeListener: Any): Unit =
		listenersPointer.update { _.map { _.filterNot { _ == changeListener } } }
	override protected def removeListeners(priority: End, listenersToRemove: Vector[ChangeListener[A]]): Unit =
		listenersPointer.update { _.mapSide(priority) { _.filterNot(listenersToRemove.contains) } }
}
