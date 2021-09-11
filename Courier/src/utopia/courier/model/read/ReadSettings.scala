package utopia.courier.model.read

import utopia.courier.model.{Authentication, EmailSettings}

/**
  * A trait common for mail read setting implementations
  * @author Mikko Hilpinen
  * @since 10.9.2021, v0.1
  */
trait ReadSettings extends EmailSettings
{
	/**
	  * @return The name of the "store" used with these settings
	  */
	def storeName: String
	
	/**
	  * @return Authentication used for accessing the emailing service
	  */
	def authentication: Authentication
	
	override def removedProperties = Set()
}
