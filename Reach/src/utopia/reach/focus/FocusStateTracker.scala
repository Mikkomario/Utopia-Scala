package utopia.reach.focus

import utopia.firmament.context.ComponentCreationDefaults.componentLogger
import utopia.flow.view.mutable.eventful.ResettableFlag

/**
  * A focus listener used for tracking focus status
  * @author Mikko Hilpinen
  * @since 4.11.2020, v0.1
  */
class FocusStateTracker(hasFocusInitially: Boolean) extends FocusChangeListener with FocusTracking
{
	// ATTRIBUTES	-------------------------
	
	private val pointer = ResettableFlag(hasFocusInitially)
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return A pointer to the tracked focus state
	  */
	def focusPointer = pointer.view
	
	
	// IMPLEMENTED	-------------------------
	
	/**
	  * @return Whether the tracked component currently holds focus
	  */
	override def hasFocus = pointer.value
	
	override def onFocusChangeEvent(event: FocusChangeEvent) = pointer.value = event.hasFocus
}
