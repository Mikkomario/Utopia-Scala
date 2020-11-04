package utopia.reflection.event

/**
  * A focus listener used for tracking focus status
  * @author Mikko Hilpinen
  * @since 4.11.2020, v2
  */
class FocusStateTracker(hasFocusInitially: Boolean) extends FocusChangeListener
{
	// ATTRIBUTES	-------------------------
	
	private var state = hasFocusInitially
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether the tracked component currently holds focus
	  */
	def hasFocus = state
	
	
	// IMPLEMENTED	-------------------------
	
	override def onFocusChangeEvent(event: FocusChangeEvent) = state = event.hasFocus
}
