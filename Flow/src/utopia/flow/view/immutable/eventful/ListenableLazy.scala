package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener, LazyListener}
import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.eventful.ChangingLike

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

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
		
		override def stateView: ChangingLike[Option[A]] = StateView
		
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
		
		override def map[B](f: A => B) = ListenableLazy { f(value) }
		
		
		// NESTED   --------------------------------
		
		private object StateView extends ChangingLike[Option[A]]
		{
			// ATTRIBUTES   ------------------------
			
			private var queuedDependencies = Vector[ChangeDependency[Option[A]]]()
			private var queuedListeners = Vector[ChangeListener[Option[A]]]()
			
			
			// IMPLEMENTED  ------------------------
			
			override def value = current
			
			override def isChanging = nonInitialized
			
			override def addListener(changeListener: => ChangeListener[Option[A]]) =
				if (nonInitialized) queuedListeners :+= changeListener
			
			override def addListenerAndSimulateEvent[B >: Option[A]](simulatedOldValue: B)
			                                                        (changeListener: => ChangeListener[B]) =
			{
				if (simulatedOldValue == None)
					addListener(changeListener)
				else
				{
					val listener = changeListener
					current match
					{
						case Some(value) => listener.onChangeEvent(ChangeEvent(simulatedOldValue, Some(value)))
						case None =>
							listener.onChangeEvent(ChangeEvent(simulatedOldValue, None))
							addListener(listener)
					}
				}
			}
			
			override def removeListener(changeListener: Any) =
				queuedListeners = queuedListeners.filterNot { _ == changeListener }
			
			override def addDependency(dependency: => ChangeDependency[Option[A]]) =
				if (nonInitialized) queuedDependencies :+= dependency
			
			override def removeDependency(dependency: Any) =
				queuedDependencies = queuedDependencies.filterNot { _ == dependency }
			
			/* Removed 12.9.2022 because ChangingLike now implements this without using an ExecutionContext
			override def futureWhere(valueCondition: Option[A] => Boolean)(implicit exc: ExecutionContext) =
				current match
				{
					case Some(value) => if (valueCondition(Some(value))) Future.successful(Some(value)) else Future.never
					case None =>
						if (valueCondition(None))
							Future.successful(None)
						else
							valueFuture.flatMap { value =>
								if (valueCondition(Some(value)))
									Future.successful(Some(value))
								else
									Future.never
							}
				}
			 */
			
			override def map[B](f: Option[A] => B) = Mirror.of(this)(f)
			
			override def lazyMap[B](f: Option[A] => B) = LazyMirror.of(this)(f)
			
			override def mergeWith[B, R](other: ChangingLike[B])(f: (Option[A], B) => R) =
				MergeMirror.of(this, other)(f)
			
			override def mergeWith[B, C, R](first: ChangingLike[B], second: ChangingLike[C])(merge: (Option[A], B, C) => R) =
				TripleMergeMirror.of(this, first, second)(merge)
			
			override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (Option[A], B) => R) =
				LazyMergeMirror.of(this, other)(f)
			
			override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext) =
				DelayedView.of(this, threshold)
			
			
			// OTHER    -------------------------------
			
			def unqueueListeners(newValue: A) =
			{
				val event = ChangeEvent(None, Some(newValue))
				
				val dependencies = queuedDependencies
				queuedDependencies = Vector()
				val actions = dependencies.flatMap { _.beforeChangeEvent(event) }
				
				val listeners = queuedListeners
				queuedListeners = Vector()
				listeners.foreach { _.onChangeEvent(event) }
				actions.foreach { _() }
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
	def stateView: ChangingLike[Option[A]]
	
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
	def map[B](f: A => B): ListenableLazy[B]
	
	
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
