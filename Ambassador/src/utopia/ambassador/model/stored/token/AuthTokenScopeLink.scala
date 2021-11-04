package utopia.ambassador.model.stored.token

import utopia.ambassador.database.access.single.token.DbSingleAuthTokenScopeLink
import utopia.ambassador.model.partial.token.AuthTokenScopeLinkData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthTokenScopeLink that has already been stored in the database
  * @param id id of this AuthTokenScopeLink in the database
  * @param data Wrapped AuthTokenScopeLink data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthTokenScopeLink(id: Int, data: AuthTokenScopeLinkData) 
	extends StoredModelConvertible[AuthTokenScopeLinkData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthTokenScopeLink in the database
	  */
	def access = DbSingleAuthTokenScopeLink(id)
}

