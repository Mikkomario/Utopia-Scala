package utopia.ambassador.database

import utopia.ambassador.database.access.many.scope.DbScopes
import utopia.ambassador.database.access.many.token.DbAuthTokens
import utopia.ambassador.rest.util.AuthUtils
import utopia.citadel.database.access.single.organization.{DbSingleTask, DbTask}
import utopia.citadel.database.access.single.user.DbSingleUser
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
		def authTokens = DbAuthTokens.forUserWithId(a.id)
		
		/**
		  * @param connection Implicit database connection
		  * @return Ids of the scopes that are currently available for this user without a new OAuth process
		  */
		def accessibleScopeIds(implicit connection: Connection) = authTokens.scopeIds
		
		
		// OTHER ----------------------------
		
		/**
		 * Checks whether this user is authorized to perform the specified task.
		 * Only considers 3rd party service authentication. Doesn't check for organization memberships.
		 * @param taskId Id of the task being performed
		 * @param connection Implicit DB Connection
		 * @return Whether this user is authorized to perform the specified task
		 */
		def isAuthorizedForTaskWithId(taskId: Int)(implicit connection: Connection) =
			AuthUtils.testTaskAccess(DbTask(taskId).scopes.pull, accessibleScopeIds)
		
		/**
		 * Checks whether this user is authorized to perform the specified task when only one service is considered
		 * @param serviceId Id of the targeted service
		 * @param taskId Id of the targeted task
		 * @param connection Implicit DB Connection
		 * @return Whether this user has access to perform that task when that service is considered
		 */
		def isAuthorizedForServiceTask(serviceId: Int, taskId: Int)(implicit connection: Connection) =
			AuthUtils.testTaskAccess(DbTask(taskId).scopes.forServiceWithId(serviceId), accessibleScopeIds)
	}
	
	implicit class DbAuthTask(val a: DbSingleTask) extends AnyVal
	{
		/**
		  * @return An access point to this task's scopes
		  */
		def scopes = DbScopes.forTaskWithId(a.id)
	}
}
