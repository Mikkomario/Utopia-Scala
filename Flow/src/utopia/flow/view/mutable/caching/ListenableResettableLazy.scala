package utopia.flow.view.mutable.caching

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.{ChangingStoppedListener, LazyListener, LazyResetListener, ResettableLazyListener}
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.view.immutable.eventful.ListenableLazy
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{AbstractChanging, ResetListenable}

import scala.concurrent.{Future, Promise}

object ListenableResettableLazy
{
	// OTHER    ------------------------
	
	/**
	  * @param make A function for generating a new value when one is requested
	  * @tparam A Type of stored value
	  * @return A new resettable lazy container
	  */
	def apply[A](make: => A): ListenableResettableLazy[A] = new _ListenableResettableLazy[A](make)
	
	
	// NESTED   ----------------------
	
	private class _ListenableResettableLazy[A](generator: => A) extends ListenableResettableLazy[A]
	{
		// ATTRIBUTES   ------------------------------
		
		private var _value: Option[A] = None
		
		private val nextValuePromisePointer = Pointer.option[Promise[A]]()
		private var generationListeners: Seq[LazyListener[A]] = Empty
		private var resetListeners: Seq[LazyResetListener[A]] = Empty
		
		
		// COMPUTED ----------------------------------
		
		/**
		  * @return Future of the <b>next</b> value assigned to this lazy container.
		  *         Doesn't recognize the current value.
		  * @see .valueFuture
		  */
		def nextValueFuture = nextValuePromisePointer.getOrElseUpdate { Promise[A]() }.future
		
		
		// IMPLEMENTED  ------------------------------
		
		override def stateView: AbstractChanging[Option[A]] = StateView
		
		override def current = _value
		
		override def value = _value.getOrElse {
			// Generates and stores a new value
			val newValue = generator
			_value = Some(newValue)
			// Informs the listeners
			nextValuePromisePointer.pop().foreach { _.success(newValue) }
			generationListeners.foreach { _.onValueGenerated(newValue) }
			StateView.onValueGenerated()
			// Returns the new value
			newValue
		}
		
		override def valueFuture = _value match {
			case Some(value) => Future.successful(value)
			case None => nextValueFuture
		}
		
		override def reset() = {
			val wasSet = _value.isDefined
			_value.foreach { oldValue =>
				// Clears the current value, then informs the listeners about the change
				_value = None
				resetListeners.foreach { _.onReset(oldValue) }
				StateView.onValueReset(oldValue)
			}
			wasSet
		}
		
		override def addListener(listener: => LazyListener[A]) = listener match {
			case resetListener: ResettableLazyListener[A] =>
				generationListeners :+= resetListener
				resetListeners :+= resetListener
			case valueListener: LazyListener[A] => generationListeners :+= valueListener
		}
		override def removeListener(listener: Any) =
		{
			generationListeners = generationListeners.filterNot { _ == listener }
			resetListeners = resetListeners.filterNot { _ == listener }
		}
		
		override def addResetListener(listener: LazyResetListener[A]) = {
			if (!resetListeners.contains(listener))
				resetListeners :+= listener
		}
		
		override def removeResetListener(listener: Any): Unit =
			resetListeners = resetListeners.filterNot { _ == listener }
		
		override protected def mapToListenable[B](f: A => B) = {
			val newLazy = ListenableResettableLazy { f(value) }
			addResetListener(LazyResetListener.onAnyReset { newLazy.reset() })
			newLazy
		}
		
		override def mapValue[B](f: A => B): ListenableLazy[B] = mapToListenable(f)
		
		
		// NESTED   -------------------------------
		
		private object StateView extends AbstractChanging[Option[A]]
		{
			// IMPLEMENTED  ----------------------
			
			override def value = current
			override def destiny: Destiny = ForeverFlux
			
			override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
			
			
			// OTHER    --------------------------
			
			def onValueGenerated() = fireEventIfNecessary(None).foreach { _() }
			def onValueReset(oldValue: A) = fireEventIfNecessary(Some(oldValue)).foreach { _() }
		}
	}
}

/**
  * A lazy container that allows one to reset the value so that it is generated again.
  * Also informs a set of listeners whenever new values are generated and when this container is reset.
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait ListenableResettableLazy[A] extends ResettableLazy[A] with ListenableLazy[A] with ResetListenable[A]
{
	// ABSTRACT ----------------------------------
	
	/**
	  * @return Future of the <b>next</b> value assigned to this lazy container.
	  *         Doesn't recognize the current value.
	  * @see .valueFuture
	  */
	def nextValueFuture: Future[A]
	
	
	// IMPLEMENTED  ------------------------------
	
	override def addResetListenerAndSimulateEvent[B >: A](simulatedOldValue: => B)(listener: LazyResetListener[B]): Unit = {
		if (current.isEmpty)
			listener.onReset(simulatedOldValue)
		addResetListener(listener)
	}
}
