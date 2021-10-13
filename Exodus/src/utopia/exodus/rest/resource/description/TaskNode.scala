package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.access.http.Status.NotFound
import utopia.citadel.database.access.many.description.{DbDescriptionRoles, DbTaskDescriptions}
import utopia.citadel.database.access.single.organization.DbTask
import utopia.exodus.rest.resource.scalable.{ExtendableSessionResource, ExtendableSessionResourceFactory, SessionUseCaseImplementation}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedTask
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object TaskNode extends ExtendableSessionResourceFactory[Int, TaskNode]
{
	override protected def buildBase(param: Int) = new TaskNode(param)
}

/**
  * Used for accessing data concerning individual tasks (extendable)
  * @author Mikko Hilpinen
  * @since 19.7.2021, v2.0.1
  */
class TaskNode(val taskId: Int) extends ExtendableSessionResource
{
	// ATTRIBUTES   ------------------------------
	
	private val defaultGet = SessionUseCaseImplementation.default(Get) { (session, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val cntx: AuthorizedContext = context
		
		// Makes sure this task id is valid
		if (DbTask(taskId).nonEmpty)
		{
			// Reads the descriptions of this task
			val descriptions = DbTaskDescriptions(taskId)
				.inLanguages(context.languageIdListFor(session.userId))
			val task = DescribedTask(taskId, descriptions.toSet)
			// Forms a response based on this described task
			Result.Success(session.modelStyle match
			{
				case Simple => task.toSimpleModelUsing(DbDescriptionRoles.pull)
				case Full => task.toModel
			})
		}
		else
			Result.Failure(NotFound, s"$taskId is not a valid task id")
	}
	
	
	// IMPLEMENTED  ------------------------------
	
	override def name = taskId.toString
	
	override protected def defaultUseCaseImplementations = Vector(defaultGet)
	
	override protected def defaultFollowImplementations = Vector()
}
