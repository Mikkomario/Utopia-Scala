package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.TaskFactory
import utopia.citadel.database.model.organization.TaskModel
import utopia.metropolis.model.stored.organization.Task
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Tasks
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbTask extends SingleRowModelAccess[Task] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TaskModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TaskFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Task instance
	  * @return An access point to that Task
	  */
	def apply(id: Int) = DbSingleTask(id)
}

