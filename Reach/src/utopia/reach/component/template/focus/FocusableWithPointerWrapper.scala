package utopia.reach.component.template.focus

/**
  * A wrapper for a focusable component which tracks its focus state with a pointer
  * @author Mikko Hilpinen
  * @since 6.2.2021, v0.1
  */
trait FocusableWithPointerWrapper extends FocusableWrapper with FocusableWithPointer
{
	// ABSTRACT	-----------------------------
	
	override protected def focusable: FocusableWithPointer
	
	
	// IMPLEMENTED	-------------------------
	
	override def focusPointer = focusable.focusPointer
}
