package utopia.ambassador.controller.implementation

import utopia.access.http.Headers
import utopia.access.http.Method.Post
import utopia.ambassador.database.access.many.token.DbAuthTokens
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.access.single.token.DbAuthToken
import utopia.ambassador.database.AuthDbExtensions._
import utopia.ambassador.database.model.scope.ScopeModel
import utopia.ambassador.database.model.token.{AuthTokenModel, AuthTokenScopeLinkModel}
import utopia.ambassador.model.cached.TokenInterfaceConfiguration
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.model.combined.token.AuthTokenWithScopes
import utopia.ambassador.model.error.{NoTokenException, SettingsNotFoundException}
import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.partial.token.{AuthTokenData, AuthTokenScopeLinkData}
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.citadel.database.access.single.organization.DbTask
import utopia.disciple.http.request.{Request, StringBody}
import utopia.disciple.model.error.RequestFailedException
import utopia.exodus.util.ExodusContext.logger
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.template.MapAccess
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.vault.database.{Connection, ConnectionPool}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

/**
  * A common trait for implementations that take an authentication code and swap if for a
  * refresh and/or session token
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param configurations A map-like object for acquiring correct configurations for each targeted service.
  *                       Accepts a service id and returns the applicable configurations.
  */
class AcquireTokens(configurations: MapAccess[Int, TokenInterfaceConfiguration])
{
	/**
	  * Acquires session authentications needed to perform the specified task.
	  * Fails if proper tokens can't be acquired directly and neither
	  * by utilizing existing refresh tokens.
	  * @param userId Id of the user for which this authentication is performed
	  * @param taskId Id of the task the user wants to perform
	  * @param exc Implicit execution context
	  * @param connection Implicit DB Connection (used during initial token acquisition)
	  * @param connectionPool Implicit connection pool (used when saving acquired session tokens to DB upon refresh)
	  * @return Future containing a map where keys are target service ids and values are the session tokens
	  *         to use in those services. May contain a failure.
	  */
	def forTask(userId: Int, taskId: Int)
	           (implicit exc: ExecutionContext, connection: Connection, connectionPool: ConnectionPool) =
	{
		val scopes = DbTask(taskId).scopes.pull
		forTaskScopes(userId, scopes)
	}
	
	/**
	  * Acquires a valid session token to perform the specified task
	  * @param userId Id of the user who is about to perform the task
	  * @param serviceId Id of the service to which the user needs to authenticate
	  * @param taskId Id of the task the user needs to perform
	  * @param exc Implicit execution context
	  * @param connection Implicit connection
	  * @param connectionPool Implicit connection pool (used when handling possible token refresh results)
	  * @return None if no authentication token is necessary.
	  *         Otherwise Some with a future that will contain either the acquired token or a failure
	  */
	def forServiceTask(userId: Int, serviceId: Int, taskId: Int)
	                  (implicit exc: ExecutionContext, connection: Connection, connectionPool: ConnectionPool) =
	{
		// Reads the required scopes first
		val scopes = DbTask(taskId).scopes.forServiceWithId(serviceId)
		// Case: No scopes are required => returns None
		if (scopes.isEmpty)
			None
		// Case: Some scopes need to be accessed => Acquires the token
		else
		{
			// Since all the scopes are within a single service, can simplify the response a little bit
			val tokensFuture = forTaskScopes(userId, scopes).mapIfSuccess { _.head._2 }
			Some(tokensFuture)
		}
	}
	
	/**
	  * Acquires session authentications needed to perform the specified tasks,
	  * which are represented by their scope links. Fails if proper tokens can't be acquired directly and neither
	  * by utilizing existing refresh tokens.
	  * @param userId Id of the user for which this authentication is performed
	  * @param taskScopes Scopes of the tasks that need to be accessed
	  * @param exc Implicit execution context
	  * @param connection Implicit DB Connection (used during initial token acquisition)
	  * @param connectionPool Implicit connection pool (used when saving acquired session tokens to DB upon refresh)
	  * @return Future containing a map where keys are target service ids and values are the session tokens
	  *         to use in those services. May contain a failure.
	  */
	def forTaskScopes(userId: Int, taskScopes: Iterable[TaskScope])
	                 (implicit exc: ExecutionContext, connection: Connection,
	                  connectionPool: ConnectionPool): Future[Try[Map[Int, AuthTokenWithScopes]]] =
	{
		// Prepares one service at a time
		val preparationResults = taskScopes.groupBy { _.serviceId }.toVector.tryMap { case (serviceId, scopes) =>
			// Checks for existing tokens
			val tokens = DbAuthTokens.forUserWithId(userId).withScopes.forServiceWithId(serviceId).pull
			// Checks which of the scopes fulfill the set requirements
			val (alternativeScopes, requiredScopes) = scopes.divideBy { _.isRequired }
			val validTokens = tokens.filter { token => token.containsAllScopeIds(requiredScopes.map { _.scope.id }) &&
				(alternativeScopes.isEmpty || token.containsAnyOfScopeIds(alternativeScopes.map { _.scope.id })) }
			
			validTokens.find { _.isSessionToken } match
			{
				// Case 1: There exists a valid session token already => uses that
				case Some(sessionToken) => Success(Right(serviceId -> sessionToken))
				case None =>
					validTokens.headOption match
					{
						// Case 2: There exists a valid refresh token => uses it to acquire a new session token
						case Some(refreshToken) => Success(Left(serviceId -> refreshToken))
						// Case 3: Neither => Fails
						case None => Failure(new NoTokenException(s"None of the user $userId's ${
							tokens.size} tokens is able to meet the requested scopes"))
					}
			}
		}
		// Refreshes the tokens wherever it is necessary
		preparationResults.map { tokens =>
			// If there are multiple services to use, acquires the session tokens simultaneously
			val tokenFutures = tokens.map {
				case Right((serviceId, sessionToken)) => serviceId -> Future.successful(Success(sessionToken))
				case Left((serviceId, refreshToken)) =>
					// Requires service settings access
					serviceId -> DbAuthService(serviceId).settings.pull
						.toTry { new SettingsNotFoundException(s"No settings available for service $serviceId") }
						.map { settings =>
							val config = configurations(serviceId)
							val request = createRequest(settings, "refresh_token",
								"refresh_token", refreshToken.token.token, config.useAuthorizationHeader)
							requestWith(config, request, serviceId, userId, Some(refreshToken),
								settings.defaultSessionDuration)
								.tryMapIfSuccess { newTokens =>
									newTokens.find { _.isSessionToken }
										.toTry { new NoTokenException(
											s"No session token could be acquired with a refresh at service $serviceId") }
								}
						}.flattenToFuture
			}
			// Combines the futures into one, if there are many
			// Case: No authentication was necessary => returns an empty map
			if (tokenFutures.isEmpty)
				Future.successful(Success(Map[Int, AuthTokenWithScopes]()))
			// Case: Only one service was used => Wraps the token as a map
			else if (tokenFutures hasSize 1) {
				val (serviceId, tokenFuture) = tokenFutures.head
				tokenFuture.mapIfSuccess { token => Map(serviceId -> token) }
			}
			// Case: Multiple services were targeted => groups the results to a map in a separate future
			else
				Future {
					tokenFutures
						.tryMap { case (serviceId, tokenFuture) =>
							tokenFuture.waitForResult().map { serviceId -> _ } }
						.map { _.toMap }
				}
		}.flattenToFuture
	}
	
	/**
	  * Swaps an authentication code for access token(s)
	  * @param userId Id of the authenticating user
	  * @param code Acquired authentication code
	  * @param settings Service settings to use
	  * @param exc Implicit execution context
	  * @param connectionPool Implicit connection pool
	  * @return Acquired scopes. Failure if something went wrong. The result is asynchronous (Future).
	  */
	def forCode(userId: Int, code: String, settings: AuthServiceSettings)
	           (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		val config = configurations(settings.serviceId)
		// Forms the token request first
		val tokenRequest = createRequest(settings, "authorization_code", "code", code,
			config.useAuthorizationHeader)
		// Acquires the tokens using that request
		requestWith(config, tokenRequest, settings.serviceId, userId, None, settings.defaultSessionDuration)
	}
	
	private def createRequest(settings: AuthServiceSettings, grantType: String,
	                          authParamName: String, authParamValue: String, useAuthorizationHeader: Boolean) =
	{
		// Puts the client id and client secret either to the basic auth header or to the body
		val headers = {
			if (useAuthorizationHeader)
				Headers.withBasicAuthorization(settings.clientId, settings.clientSecret)
			else Headers.empty
		}
		val baseBodyModel = Model(Vector(authParamName -> authParamValue, "grant_type" -> grantType,
			"redirect_uri" -> settings.redirectUrl))
		val bodyModel = if (useAuthorizationHeader) baseBodyModel else
			baseBodyModel ++ Vector("client_id" -> settings.clientId, "client_secret" -> settings.clientSecret)
				.map { case (key, value) =>Constant(key, value) }
		
		Request(settings.tokenUrl, Post, headers = headers, body = Some(StringBody.urlEncodedForm(bodyModel)))
	}
	
	private def requestWith(config: TokenInterfaceConfiguration, request: Request, serviceId: Int, userId: Int,
	                        existingRefreshToken: Option[AuthTokenWithScopes] = None,
	                        defaultSessionDuration: => FiniteDuration = 22.hours)
	           (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		// Sends the request and handles the result once it arrives
		val requestTime = Now.toInstant
		config.gateway.tryModelResponseFor(request).tryMapIfSuccess { response =>
			// Case: Success => Parses token(s) and saves them
			if (response.isSuccess) {
				response.body.flatMap { body =>
					val containsAccessToken = body.containsNonEmpty("access_token")
					val containsRefreshToken = body.containsNonEmpty("refresh_token")
					
					if (containsAccessToken || containsRefreshToken) {
						val expiration = requestTime + body("expires_in").int.map { _.seconds }
							.getOrElse(defaultSessionDuration)
						val rawScopes = body("scope").string match {
							case Some(scope) => scope.split(" ").toVector.map { _.trim }.filter { _.nonEmpty }
							case None =>
								logger(new NoSuchElementException(
									s"No 'scope' attribute in response body. Available properties: [${
										body.nonEmptyProperties.map { _.name }.mkString(", ")
									}]"),
									"No scope attribute in token response body")
								Vector()
						}
						
						connectionPool.tryWith { implicit connection =>
							// Processes the scopes first
							val scopes = processScopes(serviceId, rawScopes)
							// Inserts the new token(s), along with the scopes
							val accessToken = body("access_token").string.map { token =>
								insertToken(userId, token, scopes, Some(expiration))
							}
							val refreshToken = body("refresh_token").string.map { token =>
								def insertNew() = insertToken(userId, token, scopes,
									config.refreshTokenDuration.finite.map { requestTime + _ }, isRefreshToken = true)
								// Checks whether the refresh token is a duplicate with the previously used or
								// whether the previous token should be replaced
								existingRefreshToken match {
									case Some(existingToken) =>
										// Case: The new token is a duplicate of the previous => doesn't insert a new token
										if (existingToken.token.token == token) {
											// May extend the scopes of the existing token
											val newScopes = scopes
												.filterNot { scope => existingToken.scopes.exists { _.id == scope.id } }
											if (newScopes.nonEmpty)
												AuthTokenScopeLinkModel.insert(
													newScopes.map { s => AuthTokenScopeLinkData(existingToken.id, s.id) })
											existingToken
										}
										// Case: New token is different => overwrites the previous token
										else {
											DbAuthToken(existingToken.id).deprecate()
											insertNew()
										}
									// Case: There was no previous refresh token => inserts a new token
									case None => insertNew()
								}
							}
							// Returns the tokens and the scopes
							Vector(refreshToken, accessToken).flatten
						}
					}
					else
						Failure(new NoSuchElementException(
							s"No 'access_token' or 'refresh_token' property in the response. Available keys: [${
								body.nonEmptyProperties.map { _.name }.mkString(", ")
							}]"))
				}
			}
			// Case: Failure
			else {
				val message = response.body match {
					case Success(body) =>
						body("error").string.orElse { body("message").string }
							.getOrElse { s"Service responded with ${ response.status }" }
					case Failure(error) =>
						s"Can't parse response body (${ error.getMessage }). Response status is ${ response.status }"
				}
				Failure(new RequestFailedException(message))
			}
		}
	}
	
	private def processScopes(serviceId: Int, scopes: Vector[String])(implicit connection: Connection) =
	{
		// Matches with existing scopes
		val existingScopes = DbAuthService(serviceId).scopes.matchingAnyOfNames(scopes).pull
		// Checks if there were new scopes introduced and saves those to the DB if necessary
		val newScopes = scopes.filterNot { scopeName => existingScopes.exists { _.name == scopeName } }
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
		val insertedToken = AuthTokenModel.insert(AuthTokenData(userId, token, expires = expiration,
			isRefreshToken = isRefreshToken))
		// Then inserts the scopes for the token
		AuthTokenScopeLinkModel.insert(scopes.map { s => AuthTokenScopeLinkData(insertedToken.id, s.id) })
		insertedToken.withScopes(scopes.toSet)
	}
}
