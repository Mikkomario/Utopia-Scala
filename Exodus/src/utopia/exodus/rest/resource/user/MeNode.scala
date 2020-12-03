package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Get
import utopia.access.http.Status.Unauthorized
import utopia.exodus.database.access.single.DbUser
import utopia.exodus.rest.resource.ResourceWithChildren
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * This rest-resource represents the logged user
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
object MeNode extends ResourceWithChildren[AuthorizedContext]
{
	override val name = "me"
	
	override val children = Vector(MyInvitationsNode, MyOrganizationsNode, MyLanguagesNode, MySettingsNode,
		MyPasswordResetNode)
	
	override val allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Reads user data and adds linked data
			DbUser(session.userId).withLinks match
			{
				case Some(user) => Result.Success(user.toModel)
				case None =>
					// Log.warning(s"User id ${session.userId} was authorized but couldn't be found from the database")
					Result.Failure(Unauthorized, "User no longer exists")
			}
		}
	}
}
