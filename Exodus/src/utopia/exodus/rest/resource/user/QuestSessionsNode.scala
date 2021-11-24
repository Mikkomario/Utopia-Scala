package utopia.exodus.rest.resource.user

import utopia.access.http.Method.{Delete, Get}
import utopia.access.http.Status.{InternalServerError, Unauthorized}
import utopia.citadel.util.CitadelContext._
import utopia.exodus.database.access.single.auth.{DbEmailValidationAttempt, DbSessionToken}
import utopia.exodus.rest.resource.NotImplementedResource
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.{LeafResource, ResourceWithChildren}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * A node used for starting and ending user sessions that are not tied to any device
  * @author Mikko Hilpinen
  * @since 8.12.2020, v1
  */
object QuestSessionsNode extends ResourceWithChildren[AuthorizedContext] with NotImplementedResource[AuthorizedContext]
{
	// ATTRIBUTES   ---------------------
	
	override val name = "quests"
	
	
	// IMPLEMENTED  ---------------------
	
	override def children = Vector(MyQuestSessionsNode)
	
	
	// NESTED   -------------------------
	
	object MyQuestSessionsNode
		extends ResourceWithChildren[AuthorizedContext] with NotImplementedResource[AuthorizedContext]
	{
		// ATTRIBUTES   -----------------
		
		override val name = "me"
		
		
		// IMPLEMENTED  -----------------
		
		override def children = Vector(MyQuestSessionTokenNode)
		
		
		// NESTED   ---------------------
		
		object MyQuestSessionTokenNode extends LeafResource[AuthorizedContext]
		{
			// ATTRIBUTES   -------------
			
			override val name = "session-token"
			override val allowedMethods = Vector(Get, Delete)
			
			
			// IMPLEMENTED  -------------
			
			override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
			{
				// On GET, acquires a new session key (uses basic authorization)
				// May also create an email validation session, if email validation token has been provided
				if (context.request.method == Get) {
					context.request.headers.bearerAuthorization match {
						// Case: Using an email token to open an email-validated session
						case Some(emailToken) =>
							connectionPool.tryWith { implicit connection =>
								DbEmailValidationAttempt.open.tokenToSession(emailToken) match {
									case Success(session) => Result.Success(session.token).toResponse
									case Failure(error) => Result.Failure(Unauthorized, error.getMessage).toResponse
								}
							}.getOrMap { error =>
								ExodusContext.handleError(error, "Failed to open an email-validated session")
								Result.Failure(InternalServerError, error.getMessage).toResponse
							}
						// Case: Using basic auth to open a session
						case None =>
							context.basicAuthorized { (userId, connection) =>
								implicit val c: Connection = connection
								val newSessionKey = DbSessionToken.forDevicelessSession(userId)
									.start(context.modelStyle).token
								Result.Success(newSessionKey)
							}
					}
				} // On DELETE, terminates the current deviceless session key (uses session authorization)
				else
					context.sessionTokenAuthorized { (session, connection) =>
						implicit val c: Connection = connection
						DbSessionToken.forDevicelessSession(session.userId).logOut()
						Result.Empty
					}
			}
		}
	}
}
