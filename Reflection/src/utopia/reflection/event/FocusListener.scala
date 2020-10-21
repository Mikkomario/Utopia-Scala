package utopia.reflection.event

/**
  * A common trait for classes interested in all kinds of focus events
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  * @see FocusChangeListener
  */
trait FocusListener
{
	/**
	  * This method is called on received focus events
	  * @param event A focus event
	  */
	def onFocusEvent(event: FocusEvent): Unit
}
