package utopia.journey.controller

import utopia.flow.container.OptionObjectFileContainer
import utopia.flow.util.FileExtensions._
import utopia.metropolis.model.combined.device.FullDevice
import utopia.journey.util.JourneySettings._

/**
  * Used for accessing information about the local client device
  * @author Mikko Hilpinen
  * @since 21.6.2020, v1
  */
object LocalDevice
{
	// ATTRIBUTES	-----------------------------
	
	private val dataContainer = new OptionObjectFileContainer[FullDevice](containersDirectory/"device.json", FullDevice)
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return Whether this device has been initialized / set up
	  */
	def isInitialized = dataContainer.nonEmpty
	
	/**
	  * @return All device data, if present
	  */
	def data = dataContainer.current
}
