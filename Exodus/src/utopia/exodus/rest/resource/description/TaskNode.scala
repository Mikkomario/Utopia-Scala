package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.access.http.Status.{NotFound, Unauthorized}
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.single.organization.DbTask
import utopia.exodus.model.enumeration.ExodusScope.ReadGeneralData
import utopia.exodus.rest.resource.scalable.{ExtendableSessionResource, ExtendableSessionResourceFactory, SessionUseCaseImplementation}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
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
	
	private val defaultGet = SessionUseCaseImplementation.default { (session, connection, context, _) =>
		implicit val c: Connection = connection
		implicit val cntx: AuthorizedContext = context
		implicit val languageIds: LanguageIds = session.languageIds
		
		// Makes sure the request is authorized for this scope
		if (session.access.hasScope(ReadGeneralData))
			// Reads task along with its descriptions
			access.described match {
				case Some(task) =>
					// Forms a response based on this described task
					Result.Success(session.modelStyle match {
						case Simple => task.toSimpleModelUsing(DbDescriptionRoles.pull)
						case Full => task.toModel
					})
				case None => Result.Failure(NotFound, s"$taskId is not a valid task id")
			}
		else
			Result.Failure(Unauthorized, "You're not authorized to read task data")
	}
	
	
	// COMPUTED ----------------------------------
	
	private def access = DbTask(taskId)
	
	
	// IMPLEMENTED  ------------------------------
	
	override def name = taskId.toString
	
	override protected def defaultUseCaseImplementations = Map(Get -> defaultGet)
	override protected def defaultFollowImplementations = Vector()
}
