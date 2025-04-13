package utopia.reach.component.template.focus

import utopia.flow.view.template.eventful.Flag

/**
  * A wrapper for a focusable component which tracks its focus state with a pointer
  * @author Mikko Hilpinen
  * @since 6.2.2021, v0.1
  */
trait FocusableWithStateWrapper extends FocusableWrapper with FocusableWithState
{
	// ABSTRACT	-----------------------------
	
	override protected def focusable: FocusableWithState
	
	
	// IMPLEMENTED	-------------------------
	
	override def focusFlag: Flag = focusable.focusFlag
}
