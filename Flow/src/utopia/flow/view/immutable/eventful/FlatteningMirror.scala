package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.FlatteningMirror.ValueUpdatingListener
import utopia.flow.view.mutable.Assignable
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

object FlatteningMirror
{
	// OTHER    ---------------------------
	
	/**
	  * Creates a new flattening mirror
	  * @param source A source pointer
	  * @param f A mapping function that potentially yields a new pointer for every change
	  * @tparam O Type of source pointer values
	  * @tparam R Type of map result pointer values
	  * @return A new pointer that always contains the value of the latest map result pointer value
	  */
	def apply[O, R](source: Changing[O])(f: O => Changing[R]) = new FlatteningMirror[O, R](source, f, None)
	
	/**
	  * Creates a new mirror that uses a mapping function that yields changing items.
	  * Additional (state) information is provided for the mapping function.
	  * @param source A source pointer that provides input for mapping
	  * @param initialMap A mapping function that takes the current value of the source pointer and converts it to
	  *                   a  result
	  * @param incrementMap A mapping function used in consecutive changes, which accepts:
	  *                         1) Previous mapping results (a pointer)
	  *                         2) A change event that occurred in the source pointer
	  *                     and yields a (new) pointer
	  * @tparam O Type of source pointer values
	  * @tparam R Type of resulting pointer values
	  * @return A new pointer that wraps the map result pointers under a single pointer interface
	  */
	def incremental[O, R](source: Changing[O])(initialMap: O => Changing[R])
	                     (incrementMap: (Changing[R], ChangeEvent[O]) => Changing[R]) =
		new FlatteningMirror[O, R](source, initialMap, Some(incrementMap))
		
	
	// NESTED   ----------------------------
	
	private class ValueUpdatingListener[-R](target: Assignable[R], activeCondition: => Boolean) extends ChangeListener[R]
	{
		override def onChangeEvent(event: ChangeEvent[R]): ChangeResponse = {
			if (activeCondition) {
				target.set(event.newValue)
				Continue
			}
			else
				Detach
		}
	}
}

/**
  * A mirror (i.e. a mapping view to a changing item) that "flattens" result,
  * i.e. handles cases where the mapping function returns changing items.
  * @author Mikko Hilpinen
  * @since 22.9.2022, v1.17
  */
class FlatteningMirror[+O, R](source: Changing[O], directMap: O => Changing[R],
                              incrementalMap: Option[(Changing[R], ChangeEvent[O]) => Changing[R]])
	extends ChangingWrapper[R]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A pointer that contains the currently tracked mid-pointer
	  */
	private val pointerPointer = incrementalMap match {
		case Some(incrementalMap) => source.incrementalMap(directMap)(incrementalMap)
		case None => source.map(directMap)
	}
	private val initialMidPointer = currentMidPointer
	/**
	  * A pointer that is used to store the currently simulated (end) value
	  */
	private val pointer = EventfulPointer[R](initialMidPointer.value)
	
	// Stop listeners are stored here while the source pointer is changing
	// Once (if) the source stops changing, transfers these over to the resulting pointer
	private val queuedStopListenersP = Volatile.emptySeq[ChangingStoppedListener]
	
	// Listener that listens to mid-pointers and updates the simulated value
	private var latestValueUpdatingListener =
		new ValueUpdatingListener[R](pointer, currentMidPointer == initialMidPointer)
	
	
	// INITIAL CODE -----------------------
	
	initialMidPointer.addListener(latestValueUpdatingListener)
	
	// Listener that listens to the source pointer and moves the aforementioned listener between mid-pointers
	pointerPointer.addListenerAndSimulateEvent(initialMidPointer, isHighPriority = true) { event =>
		// Stops tracking the old pointer
		event.oldValue.removeListener(latestValueUpdatingListener)
		
		// Starts tracking the new pointer
		val newMidPointer = event.newValue
		val newListener = new ValueUpdatingListener[R](pointer, currentMidPointer == newMidPointer)
		latestValueUpdatingListener = newListener
		newMidPointer.addListener(newListener)
		
		// Also, updates the simulated value afterwards
		Continue.and { pointer.value = newMidPointer.value }
	}
	
	// Handles source pointer stopped -case
	source.onceChangingStops {
		// Transfers the stop-listeners over
		val finalMidPointer = currentMidPointer
		queuedStopListenersP.popAll().foreach { finalMidPointer.addChangingStoppedListenerAndSimulateEvent(_) }
	}
	
	
	// COMPUTED ---------------------------
	
	private def currentMidPointer = pointerPointer.value
	
	
	// IMPLEMENTED  -----------------------
	
	override implicit def listenerLogger: Logger = source.listenerLogger
	
	// As long as the source pointer is changing, won't know whether the final result will be forever flux or sealed
	// Except that, if the source never stops changing, this pointer never stops changing either
	override def destiny = source.destiny match {
		case Sealed => currentMidPointer.destiny
		case sd => sd
	}
	
	override protected def wrapped = pointer
	override def readOnly = this
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener) = {
		if (source.mayChange)
			queuedStopListenersP :+= listener
		else
			currentMidPointer.addChangingStoppedListener(listener)
	}
}
