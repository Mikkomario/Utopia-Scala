package utopia.reflection.event

/**
  * Common trait for classes interested in focus change events
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
trait FocusChangeListener extends FocusListener
{
	// ABSTRACT	-----------------------------------------
	
	/**
	  * This method is called when a focus change event is recognized
	  * @param event A focus change event
	  */
	def onFocusChangeEvent(event: FocusChangeEvent): Unit
	
	
	// IMPLEMENTED	-------------------------------------
	
	// Ignores non-change events
	override def onFocusEvent(event: FocusEvent) = event match
	{
		case e: FocusChangeEvent => onFocusChangeEvent(e)
		case _ => ()
	}
}
