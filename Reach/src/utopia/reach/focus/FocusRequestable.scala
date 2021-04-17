package utopia.reach.focus

/**
  * A common trait for components / component managers which allow focus requesting
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
trait FocusRequestable
{
	/**
	  * Requests a focus gain for this component
	  * @param forceFocusLeave Whether focus should be forced to leave from the current focus owner (default = false)
	  * @param forceFocusEnter Whether focus should be forced to enter this component (default = false)
	  * @return Whether this component received (or is likely to receive) focus
	  */
	def requestFocus(forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false): Boolean
}
