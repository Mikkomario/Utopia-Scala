package utopia.reach.focus

import utopia.firmament.context.ComponentCreationDefaults.componentLogger
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.flow.view.template.eventful.Flag

/**
  * A focus listener used for tracking focus status
  * @author Mikko Hilpinen
  * @since 4.11.2020, v0.1
  */
class FocusStateTracker(hasFocusInitially: Boolean = false) extends FocusChangeListener with HasFocusFlag
{
	// ATTRIBUTES	-------------------------
	
	private val flag = ResettableFlag(hasFocusInitially)
	override val focusFlag: Flag = flag.view
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return A pointer to the tracked focus state
	  */
	@deprecated("Renamed to focusFlag", "v1.6")
	def focusPointer = focusFlag
	
	
	// IMPLEMENTED	-------------------------
	
	override def onFocusChangeEvent(event: FocusChangeEvent) = flag.value = event.hasFocus
}
