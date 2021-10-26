package utopia.ambassador.model.stored.token

import utopia.ambassador.database.access.single.token.DbSingleAuthToken
import utopia.ambassador.model.combined.token.AuthTokenWithScopes
import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.scope.Scope
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a AuthToken that has already been stored in the database
  * @param id id of this AuthToken in the database
  * @param data Wrapped AuthToken data
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthToken(id: Int, data: AuthTokenData) extends StoredModelConvertible[AuthTokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this AuthToken in the database
	  */
	def access = DbSingleAuthToken(id)
	
	
	// OTHER    --------------------
	
	/**
	  * @param scopes Scopes to attach to this token
	  * @return A copy of this token where the scopes have been attached
	  */
	def withScopes(scopes: Set[Scope]) = AuthTokenWithScopes(this, scopes)
}

