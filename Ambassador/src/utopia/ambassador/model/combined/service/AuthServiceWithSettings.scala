package utopia.ambassador.model.combined.service

import utopia.ambassador.model.partial.service.AuthServiceData
import utopia.ambassador.model.stored.service.{AuthService, AuthServiceSettings}
import utopia.flow.view.template.Extender

/**
  * Combines service with settings data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthServiceWithSettings(service: AuthService, settings: AuthServiceSettings) 
	extends Extender[AuthServiceData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this service in the database
	  */
	def id = service.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = service.data
}

