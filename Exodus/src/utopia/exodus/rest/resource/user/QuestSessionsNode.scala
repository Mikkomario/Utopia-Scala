package utopia.exodus.rest.resource.user

import utopia.access.http.Method.{Delete, Get}
import utopia.exodus.database.access.single.DbUserSession
import utopia.exodus.rest.resource.{NotImplementedResource, ResourceWithChildren, ResourceWithoutChildren}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext._
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.result.Result
import utopia.vault.database.Connection

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
		
		override def children = Vector(MyQuestSessionKeyNode)
		
		
		// NESTED   ---------------------
		
		object MyQuestSessionKeyNode extends ResourceWithoutChildren[AuthorizedContext]
		{
			// ATTRIBUTES   -------------
			
			override val name = "session-key"
			
			override val allowedMethods = Vector(Get, Delete)
			
			
			// IMPLEMENTED  -------------
			
			override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
			{
				// On GET, acquires a new session key (uses basic authorization)
				if (context.request.method == Get)
					context.basicAuthorized { (userId, connection) =>
						implicit val c: Connection = connection
						val newSessionKey = DbUserSession.deviceless(userId).start().key
						Result.Success(newSessionKey)
					}
				// On DELETE, terminates the current deviceless session key (uses session authorization)
				else
					context.sessionKeyAuthorized { (session, connection) =>
						implicit val c: Connection = connection
						DbUserSession.deviceless(session.userId).end()
						Result.Empty
					}
			}
		}
	}
}
