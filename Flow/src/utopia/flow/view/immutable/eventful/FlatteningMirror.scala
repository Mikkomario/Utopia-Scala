package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.{ChangingLike, ChangingWrapper}

object FlatteningMirror
{
	/**
	  * Creates a new flattening mirror
	  * @param source A source pointer
	  * @param f A mapping function that potentially yields a new pointer for every change
	  * @tparam Origin Type of source pointer values
	  * @tparam Reflection Type of map result pointer values
	  * @return A new pointer that always contains the value of the latest map result pointer value
	  */
	def apply[Origin, Reflection](source: ChangingLike[Origin])(f: Origin => ChangingLike[Reflection]) = {
		// Case: Mutating origin => Constructs a proper mirror
		if (source.isChanging)
			new FlatteningMirror[Origin, Reflection](source)(f)
		// Case: Fixed origin => Maps only once
		else
			f(source.value)
	}
}

/**
  * A mirror (i.e. a mapping view to a changing item) that "flattens" result, i.e. handles cases where the mapping
  * function returns changing items.
  * @author Mikko Hilpinen
  * @since 22.9.2022, v1.17
  */
class FlatteningMirror[Origin, Reflection](source: ChangingLike[Origin])(f: Origin => ChangingLike[Reflection])
	extends ChangingWrapper[Reflection]
{
	// ATTRIBUTES   -----------------------
	
	// Pointer that contains the currently tracked mid-pointer
	private val pointerPointer = source.map(f)
	// Pointer that contains the currently simulated value
	private val pointer = new PointerWithEvents[Reflection](pointerPointer.value.value)
	
	// Listener that listens to mid-pointers and updates the simulated value
	private val valueUpdatingListener = ChangeListener[Reflection] { event => pointer.value = event.newValue }
	
	
	// INITIAL CODE -----------------------
	
	pointerPointer.value.addListener(valueUpdatingListener)
	
	// Listener that listens to the source pointer and moves the aforementioned listener between mid-pointers
	pointerPointer.addDependency(ChangeDependency { event: ChangeEvent[ChangingLike[Reflection]] =>
		event.oldValue.removeListener(valueUpdatingListener)
		event.newValue.addListener(valueUpdatingListener)
		// Also updates the simulated value when pointers change
		Some(event.newValue.value)
	} { pointer.value = _ })
	
	
	// IMPLEMENTED  -----------------------
	
	override def isChanging = source.isChanging || pointerPointer.value.isChanging
	
	override protected def wrapped = pointer
}
