package utopia.reach.component.template

import utopia.flow.event.ChangingLike

/**
  * A common trait for focusable components which have a pointer that describes their focus state
  * @author Mikko Hilpinen
  * @since 6.2.2021, v1
  */
trait FocusableWithPointer extends FocusableWithState
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return A pointer to this component's current focus state (true when focused, false when not)
	  */
	def focusPointer: ChangingLike[Boolean]
	
	
	// IMPLEMENTED	--------------------------
	
	override def hasFocus = focusPointer.value
}
