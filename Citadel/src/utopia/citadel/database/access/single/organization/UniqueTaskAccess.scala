package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.TaskFactory
import utopia.citadel.database.model.organization.TaskModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Task
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Tasks.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueTaskAccess 
	extends SingleRowModelAccess[Task] with DistinctModelAccess[Task, Option[Task], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Time when this Task was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TaskModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TaskFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Task instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Task instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

