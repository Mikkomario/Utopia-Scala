package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Get
import utopia.access.http.Status.{Forbidden, NotFound}
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.enumeration.ModelStyle.Simple
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.{Error, Ready}
import utopia.nexus.rest.Resource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * This rest resource represents another user
 * @author Mikko Hilpinen
 * @since 7.7.2021, v2.0
 */
case class OtherUserNode(userId: Int) extends Resource[AuthorizedContext]
{
	override def name = userId.toString
	
	override def allowedMethods = Vector(Get)
	
	// Handles this path and the /settings -path using the same logic
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "settings")
			Ready(this)
		else
			Error(message = Some("Only 'settings' is available under this resource at this time"))
	}
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// GET acquires other user's current settings (simplified)
		context.sessionTokenAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Requires the requesting user to belong to a same organization with that user
			if (DbUser(session.userId).sharesOrganizationWithUserWithId(userId))
			{
				// Reads the user's settings
				DbUser(userId).settings.pull match
				{
					case Some(settings) =>
						// Supports If-Modified-Since
						if (context.request.headers.ifModifiedSince.forall { _ < settings.created })
						{
							// If /settings was targeted, may use full model style
							if (remainingPath.isEmpty || session.modelStyle == Simple)
								Result.Success(settings.toSimpleModel)
							else
								Result.Success(settings.toModel)
						}
						else
							Result.NotModified
					case None => Result.Failure(NotFound)
				}
			}
			else
				Result.Failure(Forbidden, "You don't share any organization with this user")
		}
	}
}
