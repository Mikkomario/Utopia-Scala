package utopia.ambassador.model.stored.service

import utopia.ambassador.database.access.single.service.DbSingleAuthServiceSettings
import utopia.ambassador.model.partial.service.AuthServiceSettingsData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthServiceSettings that has already been stored in the database
  * @param id id of this AuthServiceSettings in the database
  * @param data Wrapped AuthServiceSettings data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthServiceSettings(id: Int, data: AuthServiceSettingsData) 
	extends StoredModelConvertible[AuthServiceSettingsData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthServiceSettings in the database
	  */
	def access = DbSingleAuthServiceSettings(id)
}

