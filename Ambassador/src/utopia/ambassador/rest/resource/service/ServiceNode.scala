package utopia.ambassador.rest.resource.service

import utopia.access.http.Method.Get
import utopia.access.http.Status.NotFound
import utopia.ambassador.controller.implementation.AcquireTokens
import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.database.access.many.scope.DbScopeDescriptions
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.model.combined.scope.DescribedScope
import utopia.ambassador.rest.resource.service.auth.AuthNode
import utopia.ambassador.rest.util.ServiceTarget
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.caching.multi.CacheLike
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceWithChildren
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing data regarding an individual service
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
class ServiceNode(target: ServiceTarget, tokenAcquirers: CacheLike[Int, AcquireTokens],
                       redirectors: CacheLike[Int, AuthRedirector])
	extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val children = Vector(new AuthNode(target, tokenAcquirers, redirectors))
	
	
	// IMPLEMENTED  -------------------------
	
	override def name = target.toString
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Reads the service data from the DB
			target.id.flatMap { DbAuthService(_).pull } match
			{
				case Some(service) =>
					// Reads scopes and task ids
					val scopes = DbAuthService(service.id).scopes.pull
					val scopeDescriptions = DbScopeDescriptions(scopes.map { _.id }.toSet)
						.inLanguages(context.languageIdListFor { session.userId })
					val describedScopes = scopes.map { scope =>
						DescribedScope(scope, scopeDescriptions.getOrElse(scope.id, Vector()).toSet)
					}
					val taskIds = DbAuthService(service.id).taskIds
					
					// Forms a model to send back
					val style = session.modelStyle
					Result.Success(service.toModel ++ Vector(
						Constant("scopes", describedScopes.map { _.toModelWith(style) }),
						Constant("authorized_task_ids", taskIds)
					))
				case None => Result.Failure(NotFound, s"$target is not a valid service")
			}
		}
	}
}
