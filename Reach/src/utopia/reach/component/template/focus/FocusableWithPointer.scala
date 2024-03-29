package utopia.reach.component.template.focus

import utopia.flow.view.template.eventful.Changing

/**
  * A common trait for focusable components which have a pointer that describes their focus state
  * @author Mikko Hilpinen
  * @since 6.2.2021, v0.1
  */
trait FocusableWithPointer extends FocusableWithState
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return A pointer to this component's current focus state (true when focused, false when not)
	  */
	def focusPointer: Changing[Boolean]
	
	
	// IMPLEMENTED	--------------------------
	
	override def hasFocus = focusPointer.value
}
