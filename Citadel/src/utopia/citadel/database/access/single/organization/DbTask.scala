package utopia.citadel.database.access.single.organization

import utopia.citadel.database.Tables
import utopia.citadel.database.access.many.description.DbTaskDescriptions
import utopia.citadel.database.access.single.description.DbTaskDescription
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual tasks' data in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0.1
  */
object DbTask extends UnconditionalView with Indexed
{
	// IMPLEMENTED  ------------------------
	
	override def table = Tables.task
	
	override def target = table
	
	
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
		  * @return An access point to this task's individual descriptions
		  */
		def description = DbTaskDescription(taskId)
		/**
		  * @return An access point to this task's descriptions
		  */
		def descriptions = DbTaskDescriptions(taskId)
		
		
		// IMPLEMENTED  --------------------
		
		override protected def parent = DbTask
		
		override def filterCondition = index <=> taskId
	}
}
