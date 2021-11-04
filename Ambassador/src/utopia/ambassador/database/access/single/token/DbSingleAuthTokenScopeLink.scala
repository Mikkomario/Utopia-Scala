package utopia.ambassador.database.access.single.token

import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthTokenScopeLinks, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthTokenScopeLink(id: Int) 
	extends UniqueAuthTokenScopeLinkAccess with SingleIntIdModelAccess[AuthTokenScopeLink]

