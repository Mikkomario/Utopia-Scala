package utopia.ambassador.database.access.single.organization

import utopia.ambassador.database.access.many.scope.DbScopes
import utopia.citadel.database.access.many.description.DbDescriptions

/**
  * Used for accessing individual tasks' data in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbTask
{
	// OTHER    ----------------------------
	
	/**
	  * @param taskId A task id
	  * @return An access point to that task's data
	  */
	def apply(taskId: Int) = new DbSingleTask(taskId)
	
	
	// NESTED   ----------------------------
	
	class DbSingleTask(val taskId: Int)
	{
		/**
		  * @return An access point to this task's descriptions
		  */
		def descriptions = DbDescriptions.ofTaskWithId(taskId)
		
		/**
		  * @return An access point to this task's scopes
		  */
		def scopes = DbScopes.forTaskWithId(taskId)
	}
}
