package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.{Get, Put}
import utopia.access.http.Status.NotFound
import utopia.citadel.database.access.id.many.DbDescriptionRoleIds
import utopia.citadel.database.access.many.description.{DbDescriptionRoles, DbOrganizationDescriptions}
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.exodus.model.enumeration.ExodusTask.DocumentOrganization
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.SimplyDescribed
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.metropolis.model.post.NewDescription
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A rest-resource used for accessing and updating organization descriptions
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  */
case class OrganizationDescriptionsNode(organizationId: Int) extends LeafResource[AuthorizedContext]
{
	// IMPLEMENTED	-----------------------------
	
	override val name = "descriptions"
	override val allowedMethods = Pair(Get, Put)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// In GET request, reads descriptions in requested languages
		if (context.request.method == Get) {
			context.authorizedInOrganization(organizationId) { (session, _, connection) =>
				implicit val c: Connection = connection
				// Checks the languages the user wants to use and gathers descriptions in those languages
				implicit val languages: LanguageIds = session.languageIds
				val descriptions = DbOrganizationDescriptions(organizationId).inPreferredLanguages
				// Supports simple model style also
				Result.Success(session.modelStyle match {
					case Full => descriptions.map { _.toModel }.toVector
					case Simple =>
						Model.withConstants(SimplyDescribed.descriptionPropertiesFrom(
							descriptions.map { _.description }, DbDescriptionRoles.pull))
				})
			}
		}
		// In PUT request, updates descriptions based on posted model
		else {
			// Authorizes the request and parses posted description(s)
			context.authorizedForTask(organizationId, DocumentOrganization.id) { (session, _, connection) =>
				context.handlePost(NewDescription) { newDescription =>
					implicit val c: Connection = connection
					// Makes sure language id is valid
					if (DbLanguage(newDescription.languageId).nonEmpty) {
						// Updates the organization's descriptions accordingly
						val dbDescriptions = DbOrganizationDescriptions(organizationId)
						val insertedDescriptions = dbDescriptions.update(newDescription, session.ownerId)
						// Returns new version of organization's descriptions (in specified language)
						val otherDescriptions = {
							val missingRoleIds = DbDescriptionRoleIds.all.toSet --
								insertedDescriptions.map { _.description.roleId }.toSet
							if (missingRoleIds.nonEmpty)
								dbDescriptions.inLanguageWithId(newDescription.languageId)
									.withRoleIds(missingRoleIds).pull
							else
								Empty
						}
						Result.Success((insertedDescriptions ++ otherDescriptions).map { _.toModel }.toVector)
					}
					else
						Result.Failure(NotFound, s"There doesn't exist any language with id ${newDescription.languageId}")
				}
			}
		}
	}
}
