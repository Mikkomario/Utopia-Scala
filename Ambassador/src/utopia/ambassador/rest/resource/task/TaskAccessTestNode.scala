package utopia.ambassador.rest.resource.task

import utopia.access.http.Method.Get
import utopia.ambassador.database.AuthDbExtensions._
import utopia.ambassador.database.access.many.description.DbScopeDescriptions
import utopia.ambassador.database.access.many.service.DbAuthServices
import utopia.ambassador.model.combined.scope.DescribedScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.single.organization.DbTask
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.model.enumeration.ExodusScope.ReadPersonalData
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.collection.CollectionExtensions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A Rest node which simply tests whether the user has acquired access to the specified feature at this time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class TaskAccessTestNode(taskId: Int) extends LeafResource[AuthorizedContext]
{
	override def name = "access-status"
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) = {
		context.authorizedForScope(ReadPersonalData) { (session, connection) =>
			implicit val c: Connection = connection
			// Reads the scopes required by the targeted task
			val scopes = DbTask(taskId).scopes.pull
			if (scopes.nonEmpty) {
				val (alternative, required) = scopes.divideBy { _.isRequired }.toTuple
				val alternativeGroups = alternative.groupBy { _.serviceId }
				// Reads the scopes currently accessible for the user
				// (whic is only possible when the session is tied to a user)
				val readyScopeIds = session.ownerId match {
					case Some(userId) => DbUser(userId).accessibleScopeIds
					case None => Set[Int]()
				}
				
				// Checks whether there are any required and provides a description of them for the client
				val remainingRequiredScopes = required.filterNot { scope => readyScopeIds.contains(scope.id) }
				val remainingAlternativeGroups = alternativeGroups.filterNot { case (_, scopes) =>
					scopes.exists { scope => readyScopeIds.contains(scope.id) }
				}
				val hasAlternativeScopes = remainingAlternativeGroups.isEmpty
				
				if (remainingRequiredScopes.isEmpty && hasAlternativeScopes)
					Result.Success(Model(Vector("is_authorized" -> true)))
				else
				{
					// Reads the descriptions of the missing scopes
					val missingScopes = remainingRequiredScopes ++ remainingAlternativeGroups.values.flatten
					val missingScopeIds = missingScopes.map { _.id }.toSet
					implicit val languageIds: LanguageIds = session.languageIds
					val descriptions = DbScopeDescriptions(missingScopeIds).inPreferredLanguages
					// Reads service names
					val services = DbAuthServices(missingScopes.map { _.serviceId }.toSet).pull
					
					// Combines the data into a single response
					val style = session.modelStyle
					lazy val descriptionRoles = DbDescriptionRoles.pull
					def scopeToModel(scope: Scope) =
					{
						val described = DescribedScope(scope, descriptions.getOrElse(scope.id, Vector()).toSet)
						style match
						{
							case Simple => described.toSimpleModelUsing(descriptionRoles)
							case Full => described.toModel
						}
					}
					
					val serviceModels = services.map { service =>
						Model(Vector(
							"id" -> service.id,
							"name" -> service.name,
							"required" -> remainingRequiredScopes.filter { _.serviceId == service.id }
								.map { s => scopeToModel(s.scope) },
							"alternative" -> alternativeGroups.getOrElse(service.id, Vector())
								.map { s => scopeToModel(s.scope) }
						))
					}
					Result.Success(Model(Vector("is_authorized" -> false, "services" -> serviceModels)))
				}
			}
			else
				Result.Success(Model(Vector("is_authorized" -> true)))
		}
	}
}