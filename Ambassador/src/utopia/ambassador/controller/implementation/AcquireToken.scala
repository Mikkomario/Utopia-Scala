package utopia.ambassador.controller.implementation

import utopia.access.http.Headers
import utopia.access.http.Method.Post
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.model.scope.ScopeModel
import utopia.ambassador.database.model.token.{AuthTokenModel, TokenScopeLinkModel}
import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.{Request, StringBody}
import utopia.disciple.model.error.RequestFailedException
import utopia.exodus.util.ExodusContext.handleError
import utopia.flow.async.AsyncExtensions._
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.vault.database.{Connection, ConnectionPool}

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Failure

/**
  * A common trait for implementations that take an authentication code and swap if for a
  * refresh and/or session token
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param gateway Gateway for making new requests
  * @param refreshTokenDuration Duration assigned for refresh tokens (default = infinite)
  * @param useAuthorizationHeader Whether authorization header should be used for sending
  *                               client id and client secret (false if request body should be used (default))
  */
class AcquireToken(gateway: Gateway, refreshTokenDuration: Duration = Duration.Inf, useAuthorizationHeader: Boolean)
{
	/**
	  * Swaps an authentication code for access token(s)
	  * @param settings Service settings to use
	  * @param code Acquired authentication code
	  * @param userId Id of the authenticating user
	  * @param exc Implicit execution context
	  * @param connectionPool Implicit connection pool
	  * @return Acquired scopes. Failure if something went wrong. The result is asynchronous (Future).
	  */
	def forCode(settings: ServiceSettings, code: String, userId: Int)
	           (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		// Forms the token request first
		val headers = {
			if (useAuthorizationHeader)
				Headers.withBasicAuthorization(settings.clientId, settings.clientSecret)
			else Headers.empty
		}
		val baseBodyModel = Model(Vector("code" -> code, "grant_type" -> "authorization_code",
			"redirect_uri" -> settings.redirectUrl))
		val bodyModel = if (useAuthorizationHeader) baseBodyModel else
			baseBodyModel ++ Vector("client_id" -> settings.clientId, "client_secret" -> settings.clientSecret)
				.map { case (key, value) => Constant(key, value) }
		val tokenRequest = Request(settings.tokenUrl, Post, headers = headers,
			body = Some(StringBody.urlEncodedForm(bodyModel)))
		
		// Sends the request and handles the result once it arrives
		val requestTime = Now.toInstant
		gateway.modelResponseFor(tokenRequest).tryMapIfSuccess { response =>
			// Case: Success => Parses token(s) and saves them
			if (response.isSuccess)
			{
				val containsAccessToken = response.body.containsNonEmpty("access_token")
				val containsRefreshToken = response.body.containsNonEmpty("refresh_token")
				
				if (containsAccessToken || containsRefreshToken)
				{
					val expiration = requestTime + response.body("expires_in").int.map { _.seconds }
						.getOrElse { settings.defaultSessionDuration }
					val rawScopes = response.body("scope").string match
					{
						case Some(scope) => scope.split(" ").toVector.map { _.trim }.filter { _.nonEmpty }
						case None =>
							handleError(new NoSuchElementException(
								s"No 'scope' attribute in response body. Available properties: [${
									response.body.attributesWithValue.map { _.name }.mkString(", ")}]"),
								"No scope attribute in token response body")
							Vector()
					}
					
					connectionPool.tryWith { implicit connection =>
						// Processes the scopes first
						val scopes = processScopes(settings.serviceId, rawScopes)
						// Inserts the new token(s), along with the scopes
						if (containsAccessToken)
							insertToken(userId, response.body("access_token").getString, scopes, Some(expiration))
						if (containsRefreshToken)
							insertToken(userId, response.body("refresh_token").getString, scopes,
								refreshTokenDuration.finite.map { requestTime + _ }, isRefreshToken = true)
						// Returns the granted scopes
						scopes
					}
				}
				else
					Failure(new NoSuchElementException(
						s"No 'access_token' or 'refresh_token' property in the response. Available keys: [${
							response.body.attributesWithValue.map { _.name }.mkString(", ")}]"))
			}
			// Case: Failure
			else
			{
				val message = response.body("error").string.orElse { response.body("message").string }
					.getOrElse { s"Service responded with ${response.status}" }
				Failure(new RequestFailedException(message))
			}
		}
	}
	
	private def processScopes(serviceId: Int, scopes: Vector[String])(implicit connection: Connection) =
	{
		// Matches with existing scopes
		val existingScopes = DbAuthService(serviceId).scopes.matchingAnyOf(scopes)
		// Checks if there were new scopes introduced and saves those to the DB if necessary
		val newScopes = scopes.filterNot { scopeName => existingScopes.exists { _.officialName == scopeName } }
		if (newScopes.nonEmpty)
		{
			val insertedScopes = ScopeModel.insert(newScopes.map { ScopeData(serviceId, _) })
			existingScopes ++ insertedScopes
		}
		else
			existingScopes
	}
	
	private def insertToken(userId: Int, token: String, scopes: Vector[Scope], expiration: Option[Instant] = None,
	                        isRefreshToken: Boolean = false)(implicit connection: Connection) =
	{
		// Inserts the token first
		val insertedToken = AuthTokenModel.insert(AuthTokenData(userId, token, expiration = expiration,
			isRefreshToken = isRefreshToken))
		// Then inserts the scopes for the token
		TokenScopeLinkModel.insert(scopes.map { insertedToken.id -> _.id })
	}
}
