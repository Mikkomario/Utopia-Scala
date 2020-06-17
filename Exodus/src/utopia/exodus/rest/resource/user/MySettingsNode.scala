package utopia.exodus.rest.resource.user

import utopia.access.http.Method.{Get, Patch, Put}
import utopia.access.http.Status.{BadRequest, Forbidden, NotFound}
import utopia.exodus.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.post.UserSettingsUpdate
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * Used for interacting with the authorized user's settings
  * @author Mikko Hilpinen
  * @since 20.5.2020, v2
  */
object MySettingsNode extends Resource[AuthorizedContext]
{
	override val name = "settings"
	
	override val allowedMethods = Vector(Get, Put, Patch)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			val method = context.request.method
			val settings = DbUser(session.userId).settings
			// On GET request, simply reads and returns current user settings
			if (method == Get)
			{
				settings.pull match
				{
					case Some(readSettings) => Result.Success(readSettings.toModel)
					case None => Result.Failure(NotFound, "No current settings found")
				}
			}
			else
			{
				context.handlePost(UserSettingsUpdate) { update =>
					val oldSettings = settings.pull
					if (oldSettings.forall { update.isDifferentFrom(_) })
					{
						// On PUT, overwrites current settings (must be fully specified)
						// On PATCH, modifies the existing settings
						val newSettings =
						{
							if (method == Put)
								update.toPost
							else
								oldSettings.map { old => Success(update.toPatch(old)) }.getOrElse(update.toPost)
						}
						newSettings match
						{
							case Success(newSettings) =>
								settings.update(newSettings) match
								{
									case Success(inserted) => Result.Success(inserted.toModel)
									case Failure(error) => Result.Failure(Forbidden, error.getMessage)
								}
							case Failure(error) => Result.Failure(BadRequest, error.getMessage)
						}
					}
					else
						Result.Success(oldSettings.get.toModel)
				}
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(message = Some(
		"User settings doesn't have any sub-nodes at this time"))
}
