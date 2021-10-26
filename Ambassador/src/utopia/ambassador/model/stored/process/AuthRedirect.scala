package utopia.ambassador.model.stored.process

import utopia.ambassador.database.access.single.process.DbSingleAuthRedirect
import utopia.ambassador.model.partial.process.AuthRedirectData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthRedirect that has already been stored in the database
  * @param id id of this AuthRedirect in the database
  * @param data Wrapped AuthRedirect data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthRedirect(id: Int, data: AuthRedirectData) extends StoredModelConvertible[AuthRedirectData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthRedirect in the database
	  */
	def access = DbSingleAuthRedirect(id)
}

