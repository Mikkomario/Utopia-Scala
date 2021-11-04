package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleSessionToken
import utopia.exodus.model.partial.auth.SessionTokenData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a SessionToken that has already been stored in the database
  * @param id id of this SessionToken in the database
  * @param data Wrapped SessionToken data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class SessionToken(id: Int, data: SessionTokenData) extends StoredModelConvertible[SessionTokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this SessionToken in the database
	  */
	def access = DbSingleSessionToken(id)
}

