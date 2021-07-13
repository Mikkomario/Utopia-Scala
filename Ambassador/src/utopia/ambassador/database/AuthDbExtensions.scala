package utopia.ambassador.database

import utopia.ambassador.database.access.many.token.DbAuthTokens
import utopia.citadel.database.access.single.DbUser.DbSingleUser
import utopia.vault.database.Connection

/**
  * Provides extensions to Citadel and Exodus database access points
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object AuthDbExtensions
{
	implicit class DbAuthUser(val a: DbSingleUser) extends AnyVal
	{
		// COMPUTED -----------------------------
		
		/**
		  * @return An access point to this user's authentication tokens
		  */
		def authTokens = DbAuthTokens.forUserWithId(a.userId)
		
		/**
		  * @param connection Implicit database connection
		  * @return Ids of the scopes that are currently available for this user without a new OAuth process
		  */
		def accessibleScopeIds(implicit connection: Connection) = authTokens.scopeIds
	}
}
