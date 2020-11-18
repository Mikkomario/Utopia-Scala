package utopia.reflection.component.reach.template
import utopia.genesis.shape.shape1D.Direction1D
import utopia.reflection.event.FocusListener

/**
  * A wrapper class for another mutable focusable component
  * @author Mikko Hilpinen
  * @since 18.11.2020, v2
  */
trait MutableFocusableWrapper extends MutableFocusable
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @return The wrapped focusable item
	  */
	protected def focusable: MutableFocusable
	
	
	// IMPLEMENTED	-------------------------
	
	override def focusListeners_=(newListeners: Seq[FocusListener]) = focusable.focusListeners = newListeners
	
	override def focusListeners = focusable.focusListeners
	
	override def allowsFocusEnter = focusable.allowsFocusEnter
	
	override def allowsFocusLeave = focusable.allowsFocusLeave
	
	override def requestFocus(forceFocusLeave: Boolean, forceFocusEnter: Boolean) =
		focusable.requestFocus(forceFocusLeave, forceFocusEnter)
	
	override def yieldFocus(direction: Direction1D, forceFocusLeave: Boolean) =
		focusable.yieldFocus(direction, forceFocusLeave)
}
