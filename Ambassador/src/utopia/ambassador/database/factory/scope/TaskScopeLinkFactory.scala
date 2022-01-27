package utopia.ambassador.database.factory.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.scope.TaskScopeLinkData
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading TaskScopeLink data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object TaskScopeLinkFactory extends FromValidatedRowModelFactory[TaskScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def table = AmbassadorTables.taskScopeLink
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		TaskScopeLink(valid("id").getInt, TaskScopeLinkData(valid("taskId").getInt, valid("scopeId").getInt, 
			valid("isRequired").getBoolean, valid("created").getInstant))
}

