package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Post
import utopia.access.http.Status.{Created, NotFound}
import utopia.citadel.database.access.many.organization.DbOrganizations
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.post.NewOrganization
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing organization data via REST API interface
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object OrganizationsNode extends Resource[AuthorizedContext]
{
	// IMPLEMENTED	------------------------------
	
	override val name = "organizations"
	
	override val allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			context.handlePost(NewOrganization) { newOrganization =>
				implicit val c: Connection = connection
				// Checks that language id is valid, then inserts the new organization
				if (DbLanguage(newOrganization.languageId).isDefined)
				{
					val organizationId = DbOrganizations.insert(newOrganization.name, newOrganization.languageId, session.userId)
					Result.Success(organizationId, Created)
				}
				else
					Result.Failure(NotFound, s"There doesn't exist a language with id ${newOrganization.languageId}")
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		// Allows access to individual organization access points based on organization id
		path.head.int match
		{
			case Some(id) => Follow(OrganizationNode(id), path.tail)
			case None => Error(message = Some(s"${path.head} is not a valid organization id"))
		}
	}
}
