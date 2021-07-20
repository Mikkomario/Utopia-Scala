package utopia.ambassador.database

import utopia.ambassador.database.access.many.scope.DbScopes
import utopia.ambassador.database.access.many.token.DbAuthTokens
import utopia.citadel.database.access.single.DbUser.DbSingleUser
import utopia.citadel.database.access.single.organization.DbTask
import utopia.citadel.database.access.single.organization.DbTask.DbSingleTask
import utopia.flow.util.CollectionExtensions._
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
		
		
		// OTHER ----------------------------
		
		/**
		 * Checks whether this user is authorized to perform the specified task.
		 * Only considers 3rd party service authentication. Doesn't check for organization memberships.
		 * @param taskId Id of the task being performed
		 * @param connection Implicit DB Connection
		 * @return Whether this user is authorized to perform the specified task
		 */
		def isAuthorizedForTaskWithId(taskId: Int)(implicit connection: Connection) =
		{
			// Checks the scopes used by the specified task
			val taskScopes = DbTask(taskId).scopes.pull
			if (taskScopes.isEmpty)
				true
			else
			{
				// Checks the scopes available for this user at this time
				val myScopeIds = accessibleScopeIds
				// Makes sure all the required scopes are covered, and at least one alternative scope per service
				val (alternativeScopes, requiredScopes) = taskScopes.divideBy { _.isRequired }
				requiredScopes.forall { scope => myScopeIds.contains(scope.id) } &&
					(alternativeScopes.isEmpty || alternativeScopes.groupBy { _.serviceId }.values
						.forall { scopes => scopes.exists { scope => myScopeIds.contains(scope.id) } })
			}
		}
	}
	
	implicit class DbAuthTask(val a: DbSingleTask) extends AnyVal
	{
		/**
		  * @return An access point to this task's scopes
		  */
		def scopes = DbScopes.forTaskWithId(a.taskId)
	}
}
