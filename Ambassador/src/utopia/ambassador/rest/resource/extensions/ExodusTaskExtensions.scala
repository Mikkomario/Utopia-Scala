package utopia.ambassador.rest.resource.extensions

import utopia.ambassador.rest.resource.task.TaskAccessTestNode
import utopia.exodus.rest.resource.description.TaskNode
import utopia.nexus.rest.scalable.FollowImplementation

/**
  * Used for extending task-related nodes in Utopia Exodus
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object ExodusTaskExtensions
{
	private var applied = false
	
	/**
	  * Applies these extensions to the Exodus resources, if not applied already
	  */
	def apply() =
	{
		if (!applied)
		{
			applied = true
			// Adds authentication testing under task nodes
			TaskNode.addFollow { taskId => FollowImplementation.withChild(TaskAccessTestNode(taskId)) }
		}
	}
}
