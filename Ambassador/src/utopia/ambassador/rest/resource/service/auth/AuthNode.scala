package utopia.ambassador.rest.resource.service.auth

import utopia.access.http.Method.Get
import utopia.access.http.Status.{InternalServerError, NotFound}
import utopia.ambassador.controller.implementation.DefaultRedirector
import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.database.access.single.process.DbAuthPreparation
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.model.process.{AuthRedirectModel, AuthRedirectResultModel}
import utopia.ambassador.model.enumeration.GrantLevel.FullAccess
import utopia.ambassador.model.partial.process.{AuthRedirectData, AuthRedirectResultData}
import utopia.ambassador.model.stored.process.{AuthPreparation, AuthRedirect}
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.ambassador.rest.util.AuthUtils
import utopia.citadel.util.CitadelContext._
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.handleError
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.time.Now
import utopia.flow.util.CollectionExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.{Context, ResourceWithChildren}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A node used for redirecting the user to 3rd party authentication service.
  * Also contains nested features like authentication preparation
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthNode(serviceId: Int, redirector: AuthRedirector = DefaultRedirector)
	extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES   -----------------------------
	
	override val name = "auth"
	
	override lazy val children = Vector(AuthPreparationNode(serviceId))
	
	
	// IMPLEMENTED  -----------------------------
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		connectionPool.tryWith { implicit connection =>
			// Reads server settings
			DbAuthService(serviceId).settings.pull match
			{
				// Case: Settings found => processes the request
				case Some(settings) => get(settings)
				// Case: Settings not foun => fails
				case None => Result.Failure(NotFound, s"Service $serviceId is not valid or not supported at this time")
			}
		}.getOrMap { error =>
			// Case: Unexpected error => fails
			handleError(error, "Unexpected failure during an oauth redirect process")
			Result.Failure(InternalServerError, error.getMessage)
		}.toResponse
	}
	
	
	// OTHER    -----------------------------------
	
	private def get(settings: ServiceSettings)(implicit context: Context, connection: Connection) =
	{
		// Expects a valid preparation token among the query parameters
		context.request.parameters("token").string match
		{
			case Some(token) =>
				DbAuthPreparation.forToken(token) match
				{
					case Some(preparation) =>
						if (DbAuthPreparation(preparation.id).isClosed)
							AuthUtils.completionRedirect(settings, Some(preparation),
								"Authentication token was already used")
						else
						{
							// Inserts a new authentication redirect event to the DB
							val insertedRedirect = AuthRedirectModel.insert(
								AuthRedirectData(preparation.id, uuidGenerator.next(),
									Now + settings.redirectTokenDuration))
							// Redirects the user to the correct destination
							redirect(insertedRedirect, settings, preparation)
						}
					case None =>
						AuthUtils.completionRedirect(settings, None, "Invalid or expired authentication token")
				}
			case None => AuthUtils.completionRedirect(settings, None, "Authentication token is missing")
		}
	}
	
	private def redirect(event: AuthRedirect, settings: ServiceSettings, preparation: AuthPreparation)
	                    (implicit connection: Connection) =
	{
		// Checks which scopes to request
		val preparationAccess = DbAuthPreparation(preparation.id)
		val scopes = preparationAccess.requestedScopesForServiceWithId(serviceId)
		// Case: No authentication required, completes the authentication immediately
		if (scopes.isEmpty)
		{
			AuthRedirectResultModel.insert(AuthRedirectResultData(event.id, FullAccess))
			AuthUtils.completionRedirect(settings, Some(preparation))
		}
		// Case: Authentication required => Redirects the user
		else
			Result.Redirect(redirector.redirectionFor(event.token, settings, preparation, scopes))
	}
}
