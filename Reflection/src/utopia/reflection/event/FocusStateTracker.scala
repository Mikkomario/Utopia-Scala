package utopia.reflection.event

import utopia.flow.datastructure.mutable.PointerWithEvents

/**
  * A focus listener used for tracking focus status
  * @author Mikko Hilpinen
  * @since 4.11.2020, v2
  */
class FocusStateTracker(hasFocusInitially: Boolean) extends FocusChangeListener
{
	// ATTRIBUTES	-------------------------
	
	private val pointer = new PointerWithEvents(hasFocusInitially)
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return A pointer to the tracked focus state
	  */
	def focusPointer = pointer.view
	
	/**
	  * @return Whether the tracked component currently holds focus
	  */
	def hasFocus = pointer.value
	
	
	// IMPLEMENTED	-------------------------
	
	override def onFocusChangeEvent(event: FocusChangeEvent) = pointer.value = event.hasFocus
}
