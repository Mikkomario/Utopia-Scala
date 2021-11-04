package utopia.ambassador.database.access.single.token

import utopia.ambassador.database.access.many.token.DbAuthTokenScopeLinks
import utopia.ambassador.database.factory.token.AuthTokenWithScopesFactory
import utopia.ambassador.model.stored.token.AuthToken
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthTokens, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthToken(id: Int) extends UniqueAuthTokenAccess with SingleIntIdModelAccess[AuthToken]
{
	// COMPUTED -----------------------------
	
	/**
	  * @return An access point to links between this token and the allowed scopes
	  */
	def scopeLinks = DbAuthTokenScopeLinks.withTokenId(id)
	/**
	  * @return An access point to scopes allowed by this token
	  */
	def scopes = scopeLinks.withScopes
	
	/**
	  * @param connection Implicit DB Connection
	  * @return This token, along with linked scope data
	  */
	def withScopes(implicit connection: Connection) = AuthTokenWithScopesFactory.get(id)
}
