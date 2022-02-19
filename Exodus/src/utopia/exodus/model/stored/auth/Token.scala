package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleToken
import utopia.exodus.model.combined.auth.ScopedToken
import utopia.exodus.model.partial.auth.TokenData
import utopia.vault.database.Connection
import utopia.vault.model.template.Stored

/**
  * Represents a token that has already been stored in the database
  * @param id id of this token in the database
  * @param data Wrapped token data
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class Token(id: Int, data: TokenData) extends Stored[TokenData, Int]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token in the database
	  */
	def access = DbSingleToken(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return A copy of this token which includes scope link information (from DB)
	  */
	def withScopesPulled(implicit connection: Connection) = withScopes(access.scopeLinks.pull)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param scopeLinks Scope links to attach to this token
	  * @return A copy of this token which includes the specified token scope links
	  */
	def withScopes(scopeLinks: Vector[TokenScopeLink]) = ScopedToken(this, scopeLinks)
}

