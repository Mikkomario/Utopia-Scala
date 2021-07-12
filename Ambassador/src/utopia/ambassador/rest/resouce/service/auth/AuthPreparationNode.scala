package utopia.ambassador.rest.resouce.service.auth

import utopia.access.http.Method.Post
import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource

/**
  * This node is used for preparing the server to redirect the client on the next call
  * (performs authorization for the upcoming call)
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
case class AuthPreparationNode(serviceId: Int, taskId: Int) extends LeafResource[AuthorizedContext]
{
	// IMPLEMENTED  --------------------------------
	
	override def name = "preparation"
	
	override def allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			// TODO: Parse model from request and check if redirect target is present
			// Reads the scopes required by the task and those available to the user
			???
		}
	}
}
