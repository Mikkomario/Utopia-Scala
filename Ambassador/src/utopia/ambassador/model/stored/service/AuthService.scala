package utopia.ambassador.model.stored.service

import utopia.ambassador.database.access.single.service.DbSingleAuthService
import utopia.ambassador.model.partial.service.AuthServiceData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthService that has already been stored in the database
  * @param id id of this AuthService in the database
  * @param data Wrapped AuthService data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthService(id: Int, data: AuthServiceData) extends StoredModelConvertible[AuthServiceData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthService in the database
	  */
	def access = DbSingleAuthService(id)
}

