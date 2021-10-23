package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.description.ManyDescribedAccessByIds
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.metropolis.model.stored.organization.Task
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple Tasks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbTasks extends ManyTasksAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Tasks
	  * @return An access point to Tasks with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbTasksSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbTasksSubset(override val ids: Set[Int]) 
		extends ManyTasksAccess with ManyDescribedAccessByIds[Task, DescribedTask]
}

