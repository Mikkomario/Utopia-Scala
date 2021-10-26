package utopia.ambassador.model.stored.process

import utopia.ambassador.database.access.single.process.DbSingleAuthRedirectResult
import utopia.ambassador.model.partial.process.AuthRedirectResultData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthRedirectResult that has already been stored in the database
  * @param id id of this AuthRedirectResult in the database
  * @param data Wrapped AuthRedirectResult data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthRedirectResult(id: Int, data: AuthRedirectResultData) 
	extends StoredModelConvertible[AuthRedirectResultData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthRedirectResult in the database
	  */
	def access = DbSingleAuthRedirectResult(id)
}

