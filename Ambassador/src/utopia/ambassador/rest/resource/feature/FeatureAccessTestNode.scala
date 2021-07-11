package utopia.ambassador.rest.resource.feature

import utopia.access.http.Method.Get
import utopia.ambassador.database.access.single.organization.DbTask
import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.vault.database.Connection

/**
  * A Rest node which simply tests whether the user has acquired access to the specified feature at this time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class FeatureAccessTestNode(taskId: Int) extends LeafResource[AuthorizedContext]
{
	override def name = "test"
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Reads the scopes required by the targeted task
			val scopes = DbTask(taskId).scopes.pull
			
			// TODO: Check whether user has access to scopes
			// TODO: Return description of situation
			???
		}
	}
}