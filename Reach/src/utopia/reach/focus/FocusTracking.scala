package utopia.reach.focus

/**
  * A common trait for components or component managers which track their focus state
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
trait FocusTracking
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Whether this component currently has focus
	  */
	def hasFocus: Boolean
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Whether this component <b>doesn't</b> have a focus at this time
	  */
	def notInFocus = !hasFocus
}
