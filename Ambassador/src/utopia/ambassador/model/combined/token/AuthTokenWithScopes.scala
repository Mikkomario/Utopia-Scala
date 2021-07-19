package utopia.ambassador.model.combined.token

import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.util.Extender

/**
  * Combines scope information to an access or refresh token
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
case class AuthTokenWithScopes(token: AuthToken, scopes: Set[Scope]) extends Extender[AuthTokenData]
{
	// COMPUTED -----------------------------
	
	/**
	  * @return DB id of this token
	  */
	def id = token.id
	
	
	// IMPLEMENTED  -------------------------
	
	override def wrapped = token
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param searchedScopes Scopes to search
	  * @return Whether this token provides access to all of those scopes
	  */
	def containsAll(searchedScopes: Iterable[Scope]) =
		searchedScopes.forall { scope => scopes.exists { _.id == scope.id } }
	
	/**
	  * @param searchedScopes scopes to search
	  * @return Whether this token provides access to at least one of those scopes
	  */
	def containsAnyOf(searchedScopes: Iterable[Scope]) =
		searchedScopes.exists { scope => scopes.exists { _.id == scope.id } }
}
