package utopia.ambassador.database.factory.service

import utopia.ambassador.model.combined.service.AuthServiceWithSettings
import utopia.ambassador.model.stored.service.{AuthService, AuthServiceSettings}
import utopia.vault.nosql.factory.row.linked.CombiningFactory

/**
  * Used for reading AuthServiceWithSettingss from the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthServiceWithSettingsFactory 
	extends CombiningFactory[AuthServiceWithSettings, AuthService, AuthServiceSettings]
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = AuthServiceSettingsFactory
	
	override def parentFactory = AuthServiceFactory
	
	override def apply(service: AuthService, settings: AuthServiceSettings) = 
		AuthServiceWithSettings(service, settings)
}

