package utopia.flow.datastructure.immutable

import utopia.flow.async.DelayedView
import utopia.flow.datastructure.template.ListenableLazyLike
import utopia.flow.event.{ChangeDependency, ChangeEvent, ChangeListener, ChangingLike, LazyListener, LazyMergeMirror, LazyMirror, MergeMirror, Mirror, TripleMergeMirror}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

object ListenableLazy
{
	/**
	  * @param make A function for generating a new value when it is first requested
	  * @tparam A Type of value being stored
	  * @return A new lazy container
	  */
	def apply[A](make: => A) = new ListenableLazy[A](make)
}

/**
  * A view to a value that is lazily initialized. Generates value creation events also.
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
class ListenableLazy[A](generator: => A) extends ListenableLazyLike[A]
{
	// ATTRIBUTES   -------------------------------
	
	// The listeners are stored until a value is generated
	private var queuedListeners = Vector[LazyListener[A]]()
	private var generated: Option[A] = None
	
	// Value future is generated only once
	override lazy val valueFuture = generated match
	{
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
