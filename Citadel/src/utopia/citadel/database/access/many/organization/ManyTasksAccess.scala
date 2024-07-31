package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.description.{DbTaskDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.organization.TaskFactory
import utopia.citadel.database.model.organization.TaskModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.metropolis.model.stored.organization.Task
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyTasksAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyTasksAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyTasksAccess
}

/**
  * A common trait for access points which target multiple Tasks at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyTasksAccess 
	extends ManyRowModelAccess[Task] with ManyDescribedAccess[Task, DescribedTask] 
		with FilterableView[ManyTasksAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * creationTimes of the accessible Tasks
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TaskModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TaskFactory
	
	override protected def describedFactory = DescribedTask
	
	override protected def manyDescriptionsAccess = DbTaskDescriptions
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyTasksAccess = ManyTasksAccess(condition)
	
	override def idOf(item: Task) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Task instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Task instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

