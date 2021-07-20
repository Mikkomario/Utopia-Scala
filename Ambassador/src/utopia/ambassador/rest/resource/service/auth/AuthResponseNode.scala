package utopia.ambassador.rest.resource.service.auth

import utopia.access.http.Method.Get
import utopia.access.http.Status.{BadRequest, InternalServerError, NotFound, Unauthorized}
import utopia.ambassador.controller.implementation.AcquireTokens
import utopia.ambassador.database.access.single.process.{DbAuthPreparation, DbAuthRedirect}
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.model.process.{AuthRedirectResultModel, IncompleteAuthModel}
import utopia.ambassador.model.enumeration.GrantLevel
import utopia.ambassador.model.enumeration.GrantLevel.{AccessDenied, AccessFailed, FullAccess, PartialAccess}
import utopia.ambassador.model.partial.process.{AuthRedirectResultData, IncompleteAuthData}
import utopia.ambassador.model.stored.process.{AuthPreparation, AuthRedirect}
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.ambassador.rest.util.{AuthUtils, ServiceTarget}
import utopia.citadel.util.CitadelContext._
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.{handleError, uuidGenerator}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.Now
import utopia.flow.util.CollectionExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceWithChildren
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * This node receives users arriving from the 3rd party service after their authentication
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
class AuthResponseNode(target: ServiceTarget, tokenAcquirer: AcquireTokens)
	extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val children = Vector(new AuthResponseClosureNode(target, tokenAcquirer))
	
	
	// IMPLEMENTED  -------------------------
	
	override def name = "response"
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Starts by reading service settings from the database
		connectionPool.tryWith { implicit connection =>
			target.id.flatMap { DbAuthService(_).settings.pull } match
			{
				case Some(settings) =>
					// Acquires authentication code, which is present if the user provided access
					val params = context.request.parameters
					val code = params("code").string
					val errorMessage = params("error").getString
					// Expects the state parameter (token) on normal requests, requests without state are
					// handled in a different manner (incomplete auth use case)
					params("state").string match
					{
						// Case: Token provided, as it should be => authenticates this request with that token
						case Some(token) =>
							DbAuthRedirect.valid.forToken(token) match
							{
								case Some(redirect) =>
									// Makes sure the redirection hasn't been used / cosed yet
									// TODO: Could treat this case as a security risk
									if (DbAuthRedirect(redirect.id).isClosed)
										Result.Failure(Unauthorized, "Authentication was closed already")
									else
										handleDefaultCase(settings, redirect, code, errorMessage)
								case None =>
									AuthUtils.completionRedirect(settings, errorMessage = "Authentication expired",
										deniedAccess = code.isEmpty)
							}
						// Case: No token provided => treats as an incomplete authentication
						// (action determined by settings)
						case None =>
							settings.incompleteAuthUrl match
							{
								// Case: Incomplete auth process is supported
								case Some(redirectUrl) =>
									code match
									{
										case Some(code) => handleIncompleteAuthCase(settings, code, redirectUrl)
										case None =>
											AuthUtils.completionRedirect(settings,
												errorMessage = "Authentication not initiated by this service",
												deniedAccess = true)
									}
								// Case: Incomplete auth process is not supported => Fails without redirection
								case None => Result.Failure(BadRequest, "Query parameter 'state' is required")
							}
					}
				case None => Result.Failure(NotFound, s"$target is not valid or is unavailable")
			}
		}.getOrMap { error =>
			handleError(error, s"Unexpected failure during $target auth response handling")
			Result.Failure(InternalServerError, error.getMessage)
		}.toResponse
	}
	
	private def handleDefaultCase(settings: ServiceSettings, redirect: AuthRedirect, code: Option[String],
	                              errorMessage: String = "")
	                             (implicit connection: Connection) =
	{
		val serviceId = settings.serviceId
		// Reads authentication preparation data for future actions
		val preparation = DbAuthPreparation(redirect.preparationId).pull
		
		code match
		{
			// Case: Authentication code provided => Attempts to acquire token(s) using the code
			case Some(code) =>
				// Reads authentication preparation data for future actions
				preparation match
				{
					case Some(preparation) =>
						// Swaps the code for access token(s). Blocks.
						tokenAcquirer.forCode(preparation.userId, code, settings).waitForResult() match
						{
							// Case: Acquired access tokens => checks if acquired scope matches the request
							case Success(tokens) =>
								val requestedScopes = DbAuthPreparation(preparation.id)
									.requestedScopesForServiceWithId(serviceId)
								val grantLevel = if (tokens.exists { _.containsAll(requestedScopes) }) FullAccess else
									PartialAccess
								completeWithRedirectResult(settings, redirect.id, Some(preparation), grantLevel)
							// Case: Failed to acquire access tokens
							case Failure(error) =>
								handleError(error, s"Authentication to service $serviceId failed")
								completeWithRedirectResult(settings, redirect.id, Some(preparation), AccessFailed,
									error.getMessage)
						}
					// Case: No preparation found (programming error)
					case None =>
						completeWithRedirectResult(settings, redirect.id, None, AccessFailed,
							"Failed to match authentication with a proper preparation")
				}
			// Case: Authentication code not provided => Treats as a rejection of access
			case None =>
				completeWithRedirectResult(settings, redirect.id, preparation, AccessDenied, errorMessage)
		}
	}
	
	private def handleIncompleteAuthCase(settings: ServiceSettings, code: String, redirectUrl: String)
	                                    (implicit connection: Connection) =
	{
		// Opens an incomplete authentication case
		val newCase = IncompleteAuthModel.insert(IncompleteAuthData(settings.serviceId, code, uuidGenerator.next(),
			Now + settings.incompleteAuthTokenDuration))
		// Redirects the user to the incomplete authentication endpoint
		val tokenParamString = s"token=${newCase.token}"
		val finalUrl = if (redirectUrl.contains('?')) s"$redirectUrl&$tokenParamString" else
			s"$redirectUrl?$tokenParamString"
		Result.Redirect(finalUrl)
	}
	
	private def completeWithRedirectResult(settings: ServiceSettings, redirectId: Int,
	                                       preparation: Option[AuthPreparation] = None,
	                                       accessLevel: GrantLevel = FullAccess, errorMessage: String = "")
	                                      (implicit connection: Connection) =
	{
		// Inserts the result to the DB
		AuthRedirectResultModel.insert(AuthRedirectResultData(redirectId, accessLevel))
		// Creates the proper redirection
		val actualErrorMessage = {
			if (errorMessage.nonEmpty)
				errorMessage
			else
				accessLevel match
				{
					case AccessDenied => "User denied access"
					case AccessFailed => "Authentication failed"
					case _ => ""
				}
		}
		AuthUtils.completionRedirect(settings, preparation, actualErrorMessage, !accessLevel.isFull)
	}
}
