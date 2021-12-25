package utopia.citadel.database.access.many.organization

import java.time.Instant
import utopia.citadel.database.access.many.description.{DbTaskDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.organization.TaskFactory
import utopia.citadel.database.model.organization.TaskModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.metropolis.model.stored.organization.Task
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyTasksAccess
{
	// NESTED	--------------------
	
	private class ManyTasksSubView(override val parent: ManyRowModelAccess[Task], 
		override val filterCondition: Condition) 
		extends ManyTasksAccess with SubView
}

/**
  * A common trait for access points which target multiple Tasks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyTasksAccess
	extends ManyRowModelAccess[Task] with ManyDescribedAccess[Task, DescribedTask] with FilterableView[ManyTasksAccess]
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
	
	override def filter(additionalCondition: Condition): ManyTasksAccess = 
		new ManyTasksAccess.ManyTasksSubView(this, additionalCondition)
	
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

