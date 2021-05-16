package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.mutable.Settable.OptionPointer
import utopia.flow.datastructure.template.ListenableLazyLike
import utopia.flow.event.{ChangeDependency, ChangeListener, Changing, LazyListener, ResettableLazyListener}

import scala.concurrent.{Future, Promise}

/**
  * A lazy container that allows one to reset the value so that it is generated again.
  * Also informs a set of listeners whenever new values are generated and when this container is reset.
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
class ListenableResettableLazy[A](generator: => A) extends ResettableLazyLike[A] with ListenableLazyLike[A]
{
	// ATTRIBUTES   ------------------------------
	
	private var _value: Option[A] = None
	
	private val nextValuePromisePointer = Pointer.option[Promise[A]]()
	private var generationListeners = Vector[LazyListener[A]]()
	private var resetListeners = Vector[ResettableLazyListener[A]]()
	
	
	// COMPUTED ----------------------------------
	
	/**
	  * @return Future of the <b>next</b> value assigned to this lazy container.
	  *         Doesn't recognize the current value.
	  * @see .valueFuture
	  */
	def nextValueFuture = nextValuePromisePointer.getOrElseUpdate { Promise[A]() }.future
	
	
	// IMPLEMENTED  ------------------------------
	
	override def stateView: Changing[Option[A]] = StateView
	
	override def current = _value
	
	override def value = _value.getOrElse {
		// Generates and stores a new value
		val newValue = generator
		_value = Some(newValue)
		// Informs the listeners
		nextValuePromisePointer.pop().foreach { _.success(newValue) }
		generationListeners.foreach { _.onValueGenerated(newValue) }
		resetListeners.foreach { _.onValueGenerated(newValue) }
		StateView.onValueGenerated()
		// Returns the new value
		newValue
	}
	
	override def valueFuture = _value match
	{
		case Some(value) => Future.successful(value)
		case None => nextValueFuture
	}
	
	override def reset() = _value.foreach { oldValue =>
		// Clears the current value, then informs the listeners about the change
		_value = None
		resetListeners.foreach { _.onReset(oldValue) }
		StateView.onValueReset(oldValue)
	}
	
	override def addListener(listener: => LazyListener[A]) = listener match
	{
		case resetListener: ResettableLazyListener[A] => resetListeners :+= resetListener
		case valueListener: LazyListener[A] => generationListeners :+= valueListener
	}
	
	override def removeListener(listener: LazyListener[A]) =
	{
		generationListeners = generationListeners.filterNot { _ == listener }
		resetListeners = resetListeners.filterNot { _ == listener }
	}
	
	
	// NESTED   -------------------------------
	
	private object StateView extends Changing[Option[A]]
	{
		// ATTRIBUTES  -----------------------
		
		override var listeners = Vector[ChangeListener[Option[A]]]()
		override var dependencies = Vector[ChangeDependency[Option[A]]]()
		
		
		// IMPLEMENTED  ----------------------
		
		override def isChanging = true
		
		override def value = current
		
		
		// OTHER    --------------------------
		
		def onValueGenerated() = fireChangeEvent(None)
		def onValueReset(oldValue: A) = fireChangeEvent(Some(oldValue))
	}
}
