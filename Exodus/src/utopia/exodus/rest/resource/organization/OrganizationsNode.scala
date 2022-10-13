package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Post
import utopia.access.http.Status.{Created, NotFound, Unauthorized}
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.citadel.database.access.single.organization.DbOrganization
import utopia.exodus.model.enumeration.ExodusScope.CreateOrganization
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.metropolis.model.post.NewOrganization
import utopia.nexus.http.Path
import utopia.nexus.rest.ItemsByIdResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing organization data via REST API interface
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object OrganizationsNode extends ItemsByIdResource[AuthorizedContext]
{
	// IMPLEMENTED	------------------------------
	
	override val name = "organizations"
	override val allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForScope(CreateOrganization) { (session, connection) =>
			session.ownerId match {
				case Some(userId) =>
					context.handlePost(NewOrganization) { newOrganization =>
						implicit val c: Connection = connection
						// Checks that language id is valid, then inserts the new organization
						if (DbLanguage(newOrganization.languageId).nonEmpty)
						{
							val (organization, _) = DbOrganization.found(newOrganization.name,
								newOrganization.languageId, userId)
							Result.Success(session.modelStyle match
							{
								case Simple => organization.toSimpleModelUsing(DbDescriptionRoles.pull)
								case Full => organization.toModel
							}, Created)
						}
						else
							Result.Failure(NotFound,
								s"There doesn't exist a language with id ${newOrganization.languageId}")
					}
				case None => Result.Failure(Unauthorized, "Your current session doesn't specify who you are")
			}
		}
	}
	
	override protected def resourceForId(id: Value) = id.int.map(OrganizationNode.apply)
}
