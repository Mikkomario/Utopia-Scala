package utopia.flow.view.mutable.caching

import utopia.flow.event.listener.{LazyListener, LazyResetListener}
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.eventful.ListenableLazy
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.{Changing, ResetListenable}

import scala.concurrent.Future

object ListenableMutableLazy
{
	/**
	  * @param generator A value generator that is called lazily
	  * @tparam A Type of values held within this container
	  * @return A new container that initializes itself lazily, but also allows modifications
	  */
	def apply[A](generator: => A) = new ListenableMutableLazy[A](generator)
}

/**
  * Common trait for lazily initialized pointers, that also allow value modifications and support listening
  * @author Mikko Hilpinen
  * @since 25.7.2023, v2.2
  */
class ListenableMutableLazy[A](generator: => A) extends MutableLazy[A] with ListenableResettableLazy[A]
{
	// ATTRIBUTES   ------------------------
	
	private var listeners = Vector[LazyListener[A]]()
	private var resetListeners = Vector[LazyResetListener[A]]()
	
	private val statePointer = PointerWithEvents.empty[A]()
	
	override lazy val valueFuture: Future[A] = statePointer.findMapFuture(Identity)
	
	/**
	  * @return An immutable view into this lazy container
	  */
	lazy val view: ListenableLazy[A] with ResetListenable[A] = new _View()
	
	
	// IMPLEMENTED  ------------------------
	
	override def current: Option[A] = statePointer.value
	override def value: A = statePointer.value.getOrElse {
		val newValue = generator
		statePointer.value = Some(newValue)
		listeners.foreach { _.onValueGenerated(newValue) }
		newValue
	}
	override def value_=(newValue: A): Unit = {
		if (!current.contains(newValue)) {
			// Simulates a reset of the possible old value before the new value is assigned
			current.foreach { oldValue => resetListeners.foreach { _.onReset(oldValue) } }
			statePointer.value = Some(newValue)
			// Fires a value-generation event for the new value
			listeners.foreach { _.onValueGenerated(newValue) }
		}
	}
	
	override def stateView: Changing[Option[A]] = statePointer.view
	
	override def nextValueFuture: Future[A] = statePointer.findMapNextFuture(Identity)
	
	override def addListener(listener: => LazyListener[A]): Unit = {
		val l = listener
		if (!listeners.contains(l))
			listeners :+= l
	}
	override def addResetListener(listener: LazyResetListener[A]): Unit = {
		if (!resetListeners.contains(listener))
			resetListeners = resetListeners :+ listener
	}
	override def removeListener(listener: Any): Unit = listeners = listeners.filterNot { _ == listener }
	override def removeResetListener(listener: Any): Unit = resetListeners = resetListeners.filterNot { _ == listener }
	
	override def reset(): Boolean = {
		current match {
			case Some(oldValue) =>
				statePointer.value = None
				resetListeners.foreach { _.onReset(oldValue) }
				true
			case None => false
		}
	}
	
	override protected def mapToListenable[B](f: A => B): ListenableLazy[B] = {
		val newLazy = ListenableResettableLazy { f(value) }
		// TODO: Optimize?
		statePointer.addContinuousListener { e =>
			if (e.oldValue.isDefined)
				newLazy.reset()
		}
		newLazy
	}
	
	override def mapValue[B](f: A => B) = mapToListenable(f)
	
	
	// NESTED   -------------------------
	
	private class _View extends ListenableLazy[A] with ResetListenable[A]
	{
		// COMPUTED --------------------
		
		private def parent = ListenableMutableLazy.this
		
		
		// IMPLEMENTED  ----------------
		
		override def current: Option[A] = parent.current
		override def value: A = parent.value
		
		override def stateView: Changing[Option[A]] = parent.stateView
		override def valueFuture: Future[A] = parent.valueFuture
		
		override def addListener(listener: => LazyListener[A]): Unit = parent.addListener(listener)
		override def removeListener(listener: Any): Unit = parent.removeListener(listener)
		
		override def addResetListener(listener: LazyResetListener[A]): Unit = parent.addResetListener(listener)
		override def addResetListenerAndSimulateEvent[B >: A](simulatedOldValue: => B)(listener: LazyResetListener[B]): Unit =
			parent.addResetListenerAndSimulateEvent(simulatedOldValue)(listener)
		override def removeResetListener(listener: Any): Unit = parent.removeResetListener(listener)
		
		override protected def mapToListenable[B](f: A => B): ListenableLazy[B] = parent.mapToListenable(f)
	}
}
