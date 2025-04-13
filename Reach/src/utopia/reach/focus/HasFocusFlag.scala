package utopia.reach.focus

import utopia.flow.view.template.eventful.Flag

/**
  * Common trait for instances that can tell their focus state, and track it using an eventful flag
  * @author Mikko Hilpinen
  * @since 13.04.2025, v1.6
  */
trait HasFocusFlag extends FocusTracking
{
	// ABSTRACT --------------------------
	
	/**
	  * @return A flag that contains true while this component is in focus
	  */
	def focusFlag: Flag
	
	
	// IMPLEMENTED  ----------------------
	
	override def hasFocus: Boolean = focusFlag.value
}
