package utopia.reach.component.template

/**
  * A common trait for components which wrap a focusable component
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
trait FocusableWrapper extends Focusable
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return The focusable component being wrapped
	  */
	protected def focusable: Focusable
	
	
	// IMPLEMENTED	-----------------------
	
	override def focusId = focusable.focusId
	
	override def focusListeners = focusable.focusListeners
	
	override def allowsFocusEnter = focusable.allowsFocusEnter
	
	override def allowsFocusLeave = focusable.allowsFocusLeave
	
	/*
	override def requestFocus(forceFocusLeave: Boolean, forceFocusEnter: Boolean) =
		focusable.requestFocus(forceFocusLeave, forceFocusEnter)
	
	override def yieldFocus(direction: Direction1D, forceFocusLeave: Boolean) =
		focusable.yieldFocus(direction, forceFocusLeave)
	 */
}
