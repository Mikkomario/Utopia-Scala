package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.partial.organization.TaskData
import utopia.metropolis.model.stored.organization.Task
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading Task data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object TaskFactory extends FromValidatedRowModelFactory[Task]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.task
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		Task(valid("id").getInt, TaskData(valid("created").getInstant))
}

