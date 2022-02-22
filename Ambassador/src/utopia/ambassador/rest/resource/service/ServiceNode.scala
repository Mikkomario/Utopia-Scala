package utopia.ambassador.rest.resource.service

import utopia.access.http.Method.Get
import utopia.access.http.Status.NotFound
import utopia.ambassador.controller.implementation.AcquireTokens
import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.database.access.many.description.DbScopeDescriptions
import utopia.ambassador.database.access.many.scope.DbTaskScopeLinks
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.model.combined.scope.DescribedScope
import utopia.ambassador.rest.resource.service.auth.AuthNode
import utopia.ambassador.rest.util.ServiceTarget
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.exodus.model.enumeration.ExodusScope.ReadGeneralData
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.template.MapLike
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceWithChildren
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing data regarding an individual service
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
class ServiceNode(target: ServiceTarget, tokenAcquirer: AcquireTokens, redirectors: MapLike[Int, AuthRedirector])
	extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val children = Vector(new AuthNode(target, tokenAcquirer, redirectors))
	
	
	// IMPLEMENTED  -------------------------
	
	override def name = target.toString
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForScope(ReadGeneralData) { (session, connection) =>
			implicit val c: Connection = connection
			// Reads the service data from the DB
			target.access.pull match
			{
				case Some(service) =>
					// Reads scopes and task ids
					val scopes = DbAuthService(service.id).scopes.pull
					val scopeIds = scopes.map { _.id }.toSet
					implicit val languageIds: LanguageIds = session.languageIds
					val scopeDescriptions = DbScopeDescriptions(scopeIds).inPreferredLanguages
					val describedScopes = scopes.map { scope =>
						DescribedScope(scope, scopeDescriptions.getOrElse(scope.id, Vector()).toSet)
					}
					val taskIds = DbTaskScopeLinks.forAnyOfScopes(scopeIds).taskIds.distinct.sorted
					
					// Forms a model to send back
					val style = session.modelStyle
					val scopeModels = style match
					{
						case Simple =>
							val roles = DbDescriptionRoles.pull
							describedScopes.map { _.toSimpleModelUsing(roles) }
						case Full => describedScopes.map { _.toModel }
					}
					Result.Success(service.toModel ++ Vector(
						Constant("scopes", scopeModels),
						Constant("authorized_task_ids", taskIds)
					))
				case None => Result.Failure(NotFound, s"$target is not a valid service")
			}
		}
	}
}
