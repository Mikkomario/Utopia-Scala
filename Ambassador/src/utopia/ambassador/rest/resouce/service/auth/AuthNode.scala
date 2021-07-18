package utopia.ambassador.rest.resouce.service.auth

import utopia.access.http.Method.Get
import utopia.access.http.Status.{InternalServerError, NotFound, Unauthorized}
import utopia.ambassador.controller.implementation.DefaultRedirector
import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.database.access.single.process.DbAuthPreparation
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.model.process.{AuthRedirectModel, AuthRedirectResultModel}
import utopia.ambassador.model.enumeration.AuthCompletionType.Default
import utopia.ambassador.model.enumeration.GrantLevel.FullAccess
import utopia.ambassador.model.partial.process.{AuthRedirectData, AuthRedirectResultData}
import utopia.ambassador.model.stored.process.{AuthPreparation, AuthRedirect}
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.citadel.util.CitadelContext._
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.handleError
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
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
	
	override lazy val children = Vector(ServiceAuthPreparationNode(serviceId))
	
	
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
							completionRedirect(settings, Some(preparation),
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
						completionRedirect(settings, None, "Invalid or expired authentication token")
				}
			case None => completionRedirect(settings, None, "Authentication token is missing")
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
			completionRedirect(settings, Some(preparation))
		}
		// Case: Authentication required => Redirects the user
		else
			Result.Redirect(redirector.redirectionFor(event.token, settings, preparation, scopes))
	}
	
	// Handles cases where the user is immediately to be redirected back to the client
	private def completionRedirect(settings: ServiceSettings, preparation: Option[AuthPreparation] = None,
	                               errorMessage: String = "", deniedAccess: Boolean = false)
	                              (implicit connection: Connection) =
	{
		// Reads the redirection target from the preparation, if possible
		preparation
			.flatMap { preparation =>
				DbAuthPreparation(preparation.id).redirectTargets.forResult(errorMessage.isEmpty, deniedAccess)
					.maxByOption { _.resultFilter.priorityIndex }
					.map { target => target.resultFilter -> target.url }
			}
			// Alternatively uses service settings default, if available
			.orElse { settings.defaultCompletionUrl.map { Default -> _ } } match
		{
			// Case: Redirect url found
			case Some((urlFilter, baseUrl)) =>
				// May add some parameters to describe result state. Less parameters are included in more
				// specific redirect urls
				val stateParams =
				{
					if (urlFilter.deniedFilter)
						Vector()
					else
					{
						val deniedParam = Constant("denied_access", deniedAccess)
						if (urlFilter.successFilter.isDefined)
							Vector(deniedParam)
						else
							Vector(Constant("was_success", errorMessage.isEmpty), deniedParam)
					}
				}
				// Appends possible error and state parameters
				val allParams = Model.withConstants(stateParams) ++
					preparation.flatMap { _.clientState }.map { Constant("state", _) } ++
					errorMessage.notEmpty.map { Constant("error", _) }
				// Redirects the user
				val parametersString = allParams.attributesWithValue
					.map { att => s"${att.name}=${att.value.toJson}" }.mkString("&")
				val finalUrl = if (baseUrl.contains('?')) s"$baseUrl&$parametersString" else s"$baseUrl?$parametersString"
				Result.Redirect(finalUrl)
			// Case: No redirection url specified anywhere => Returns a success or a failure
			case None =>
				// Case: Immediate success
				if (errorMessage.isEmpty)
					Result.Success(Value.empty, description = Some("No authentication required"))
				// Case: Immediate failure
				else
					Result.Failure(Unauthorized, errorMessage)
		}
	}
}
