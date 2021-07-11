package utopia.ambassador.database.access.single.organization

import utopia.ambassador.database.access.many.scope.DbScopes
import utopia.citadel.database.Tables
import utopia.citadel.database.access.many.description.DbDescriptions
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual tasks' data in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbTask extends UnconditionalView with Indexed
{
	// IMPLEMENTED  ------------------------
	
	override def table = Tables.task
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param taskId A task id
	  * @return An access point to that task's data
	  */
	def apply(taskId: Int) = new DbSingleTask(taskId)
	
	
	// NESTED   ----------------------------
	
	class DbSingleTask(val taskId: Int) extends SubView
	{
		// COMPUTED -------------------------
		
		/**
		  * @return An access point to this task's descriptions
		  */
		def descriptions = DbDescriptions.ofTaskWithId(taskId)
		
		/**
		  * @return An access point to this task's scopes
		  */
		def scopes = DbScopes.forTaskWithId(taskId)
		
		
		// IMPLEMENTED  --------------------
		
		override protected def parent = DbTask
		
		override def filterCondition = index <=> taskId
	}
}
