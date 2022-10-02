package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.TaskFactory
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.partial.organization.TaskData
import utopia.metropolis.model.stored.organization.Task
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing TaskModel instances and for inserting Tasks to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object TaskModel extends DataInserter[TaskModel, Task, TaskData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Task created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Task created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = TaskFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: TaskData) = apply(None, Some(data.created))
	
	override def complete(id: Value, data: TaskData) = Task(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this Task was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A Task id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with Tasks in the database
  * @param id Task database id
  * @param created Time when this Task was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class TaskModel(id: Option[Int] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[Task]
{
	// IMPLEMENTED	--------------------
	
	override def factory = TaskModel.factory
	
	override def valueProperties = 
	{
		import TaskModel._
		Vector("id" -> id, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
}

