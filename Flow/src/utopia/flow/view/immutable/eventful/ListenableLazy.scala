package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.{ChangeListener, LazyListener}
import utopia.flow.event.model.ChangeEvent
import utopia.flow.operator.End
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{Future, Promise}

object ListenableLazy
{
	// OTHER    ----------------------
	
	/**
	  * Creates a new lazily initialized container that fires an event when it becomes initialized.
	  * The initialization occurs when the wrapped value is first requested.
	  * @param make A function for generating a new value when it is first requested
	  * @tparam A Type of value being stored
	  * @return A new lazily initialized container
	  */
	def apply[A](make: => A): ListenableLazy[A] = new _ListenableLazy[A](make)
	
	
	// NESTED   ----------------------
	
	private class _ListenableLazy[A](generator: => A) extends ListenableLazy[A]
	{
		// ATTRIBUTES   -------------------------------
		
		// The listeners are stored until a value is generated
		private var queuedListeners = Vector[LazyListener[A]]()
		private var generated: Option[A] = None
		
		// Value future is generated only once
		override lazy val valueFuture = generated match {
			case Some(value) => Future.successful(value)
			case None =>
				val promise = Promise[A]()
				addListener(promise.success)
				promise.future
		}
		
		
		// IMPLEMENTED  -------------------------------
		
		override def stateView: Changing[Option[A]] = StateView
		
		override def current = generated
		
		override def value = generated.getOrElse {
			// Generates and stores a new value
			val newValue = generator
			generated = Some(newValue)
			// Informs the listeners
			queuedListeners.foreach { _.onValueGenerated(newValue) }
			queuedListeners = Vector()
			StateView.unqueueListeners(newValue)
			// Returns the new value
			newValue
		}
		
		override def addListener(listener: => LazyListener[A]) = if (nonInitialized) queuedListeners :+= listener
		
		override def removeListener(listener: Any) =
			queuedListeners = queuedListeners.filterNot { _ == listener }
		
		override def mapToListenable[B](f: A => B) = ListenableLazy { f(value) }
		
		
		// NESTED   --------------------------------
		
		// TODO: Utilize FlatteningOncePointer
		private object StateView extends Changing[Option[A]]
		{
			// ATTRIBUTES   ------------------------
			
			private var queuedListeners = Pair.twice(Vector.empty[ChangeListener[Option[A]]])
			
			
			// IMPLEMENTED  ------------------------
			
			override def value = current
			override def isChanging = nonInitialized
			
			// WET WET
			override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[Option[A]]]): Unit = {
				if (nonInitialized) queuedListeners = queuedListeners.mapSide(priority) { q =>
					if (q.contains(lazyListener.value)) q else q :+ lazyListener.value
				}
			}
			
			override def removeListener(changeListener: Any) =
				queuedListeners = queuedListeners.map { _.filterNot { _ == changeListener } }
			
			
			// OTHER    -------------------------------
			
			def unqueueListeners(newValue: A) = {
				val event = ChangeEvent(None, Some(newValue))
				val queued = queuedListeners
				queuedListeners = Pair.twice(Vector.empty)
				queued.flatten.flatMap { _.onChangeEvent(event).afterEffects }.foreach { _() }
			}
		}
	}
}

/**
  * A common trait for lazy containers that generate events
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait ListenableLazy[+A] extends Lazy[A]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return A view to the state of this lazy, which also provides access to change events concerning this
	  *         instance's state
	  */
	def stateView: Changing[Option[A]]
	
	/**
	  * @return A future of the first available value of this lazy container
	  */
	def valueFuture: Future[A]
	
	/**
	  * Adds a new listener to this lazy container
	  * @param listener A listener to be informed whenever new values are generated
	  */
	def addListener(listener: => LazyListener[A]): Unit
	/**
	  * Removes a listener from this lazy container
	  * @param listener A listener to no longer be informed about value generations
	  */
	def removeListener(listener: Any): Unit
	
	/**
	  * Creates a new container that lazily maps the value of this lazy
	  * @param f A mapping function
	  * @tparam B Mapping function result type
	  * @return A lazily initialized container based on lazily mapped contents of this lazy
	  */
	protected def mapToListenable[B](f: A => B): ListenableLazy[B]
	
	
	// IMPLEMENTED  ------------------------
	
	// Had to use this convoluted way of overriding map because of some strange builder problems
	override def map[B](f: A => B): ListenableLazy[B] = mapToListenable(f)
	
	
	// OTHER    -------------------------------
	
	/**
	  * Adds a new listener to be informed whenever new values are generated.
	  * If a value has already been generated, informs the listener about that immediately.
	  * @param listener A listener to inform of value generation events
	  */
	def addListenerAndSimulateEvent(listener: => LazyListener[A]) = current match {
		case Some(initialized) =>
			val l = listener
			addListener(l)
			l.onValueGenerated(initialized)
		case None => addListener(listener)
	}
}
