package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.TokenScopeLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual token scope links, based on their id
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class DbSingleTokenScopeLink(id: Int) 
	extends UniqueTokenScopeLinkAccess with SingleIntIdModelAccess[TokenScopeLink]

