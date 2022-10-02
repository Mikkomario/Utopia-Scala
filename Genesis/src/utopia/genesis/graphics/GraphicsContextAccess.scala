package utopia.genesis.graphics

import utopia.flow.view.template.eventful.Changing

/**
  * Used for accessing a graphics context at any time for read operations
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
// TODO: Remove this class
trait GraphicsContextAccess
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return A readable pointer to the current graphics context
	  */
	def graphicsContextPointer: Changing[GraphicsContext]
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The current graphics context
	  */
	def graphicsContext = graphicsContextPointer.value
}
