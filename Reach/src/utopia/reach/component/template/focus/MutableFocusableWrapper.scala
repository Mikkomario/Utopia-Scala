package utopia.reach.component.template.focus

import utopia.reach.focus.FocusListener

/**
  * A wrapper class for another mutable focusable component
  * @author Mikko Hilpinen
  * @since 18.11.2020, v0.1
  */
trait MutableFocusableWrapper extends FocusableWrapper with MutableFocusable
{
	// ABSTRACT	-----------------------------
	
	override protected def focusable: MutableFocusable
	
	
	// IMPLEMENTED	-------------------------
	
	override def focusListeners_=(newListeners: Seq[FocusListener]) = focusable.focusListeners = newListeners
}
