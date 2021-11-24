package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleEmailValidatedSession
import utopia.exodus.model.partial.auth.EmailValidatedSessionData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a EmailValidatedSession that has already been stored in the database
  * @param id id of this EmailValidatedSession in the database
  * @param data Wrapped EmailValidatedSession data
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
case class EmailValidatedSession(id: Int, data: EmailValidatedSessionData) 
	extends StoredModelConvertible[EmailValidatedSessionData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this EmailValidatedSession in the database
	  */
	def access = DbSingleEmailValidatedSession(id)
}

