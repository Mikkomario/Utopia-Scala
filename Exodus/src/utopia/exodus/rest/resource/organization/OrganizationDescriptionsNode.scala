package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.{Get, Put}
import utopia.access.http.Status.NotFound
import utopia.citadel.database.access.id.many.DbDescriptionRoleIds
import utopia.citadel.database.access.many.description.DbDescriptions
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.exodus.model.enumeration.StandardTask.DocumentOrganization
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.post.NewDescription
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A rest-resource used for accessing and updating organization descriptions
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  */
case class OrganizationDescriptionsNode(organizationId: Int) extends Resource[AuthorizedContext]
{
	// IMPLEMENTED	-----------------------------
	
	override val name = "descriptions"
	
	override val allowedMethods = Vector(Get, Put)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// In GET request, reads descriptions in requested languages
		if (context.request.method == Get)
		{
			context.authorizedInOrganization(organizationId) { (session, _, connection) =>
				implicit val c: Connection = connection
				// Checks the languages the user wants to use and gathers descriptions in those languages
				val languages = context.languageIdListFor(session.userId)
				val descriptions = DbDescriptions.ofOrganizationWithId(organizationId).inLanguages(languages)
				Result.Success(descriptions.map { _.toModel })
			}
		}
		// In PUT request, updates descriptions based on posted model
		else
		{
			// Authorizes the request and parses posted description(s)
			context.authorizedForTask(organizationId, DocumentOrganization.id) { (session, _, connection) =>
				context.handlePost(NewDescription) { newDescription =>
					implicit val c: Connection = connection
					// Makes sure language id is valid
					if (DbLanguage(newDescription.languageId).isDefined)
					{
						// Updates the organization's descriptions accordingly
						val dbDescriptions = DbDescriptions.ofOrganizationWithId(organizationId)
						val insertedDescriptions = dbDescriptions.update(newDescription, session.userId)
						// Returns new version of organization's descriptions (in specified language)
						val otherDescriptions =
						{
							val missingRoleIds = DbDescriptionRoleIds.all.toSet --
								insertedDescriptions.map { _.description.roleId }.toSet
							if (missingRoleIds.nonEmpty)
								dbDescriptions.inLanguages(Vector(newDescription.languageId), missingRoleIds)
							else
								Vector()
						}
						Result.Success((insertedDescriptions ++ otherDescriptions).map { _.toModel })
					}
					else
						Result.Failure(NotFound, s"There doesn't exist any language with id ${newDescription.languageId}")
				}
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(message =
		Some("Organization descriptions doesn't have any sub-resources at this time"))
}
