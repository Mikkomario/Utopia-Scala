package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleTokenScopeLink
import utopia.exodus.model.combined.auth.TokenScope
import utopia.exodus.model.partial.auth.TokenScopeLinkData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a token scope link that has already been stored in the database
  * @param id id of this token scope link in the database
  * @param data Wrapped token scope link data
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenScopeLink(id: Int, data: TokenScopeLinkData) 
	extends StoredModelConvertible[TokenScopeLinkData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token scope link in the database
	  */
	def access = DbSingleTokenScopeLink(id)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param scope Scope information to attach
	  * @return A copy of this link with that infor attached
	  */
	def withScopeInfo(scope: Scope) = TokenScope(scope, this)
}

