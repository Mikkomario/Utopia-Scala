package utopia.ambassador.model.combined.token

import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.view.template.Extender

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
	
	/**
	 * @return This token as a string (only containing the auth token string)
	 */
	def tokenString = token.token
	
	
	// IMPLEMENTED  -------------------------
	
	override def wrapped = token
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param searchedScopeIds Ids of the scopes to search
	  * @return Whether this token provides access to all of those scopes
	  */
	def containsAllScopeIds(searchedScopeIds: Iterable[Int]) =
		searchedScopeIds.forall { id => scopes.exists { _.id == id } }
	/**
	  * @param searchedScopeIds Ids of the scopes to search
	  * @return Whether this token provides access to at least one of those scopes
	  */
	def containsAnyOfScopeIds(searchedScopeIds: Iterable[Int]) =
		searchedScopeIds.exists { id => scopes.exists { _.id == id } }
}
