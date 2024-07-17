package utopia.exodus.rest.util

import utopia.access.http.Status.{Forbidden, InternalServerError, Unauthorized}
import utopia.citadel.database.access.many.language.DbLanguages
import utopia.citadel.database.access.single.organization.DbMembership
import utopia.citadel.database.access.single.user.{DbUser, DbUserSettings}
import utopia.citadel.util.CitadelContext._
import utopia.exodus.database.access.single.auth.DbToken
import utopia.exodus.database.access.single.auth.DbToken.DbTokenMatch
import utopia.exodus.database.access.single.user.DbUserPassword
import utopia.exodus.model.combined.auth.ScopedToken
import utopia.exodus.model.enumeration.ExodusScope.{OrganizationActions, ReadOrganizationData}
import utopia.exodus.model.enumeration.ScopeIdWrapper
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.rest.util.AuthorizedContext.acceptLanguageIdsHeaderName
import utopia.exodus.util.ExodusContext.logger
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.NotEmpty
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.nexus.http.{Request, Response, ServerSettings}
import utopia.nexus.rest.PostContext
import utopia.nexus.result.{Result, ResultParser, UseRawJson}
import utopia.vault.database.Connection

object AuthorizedContext
{
	// ATTRIBUTES   -----------------------------
	
	/**
	  * Name of the header that may be used for specifying accepted language ids
	  */
	val acceptLanguageIdsHeaderName = "X-Accept-Language-Ids"
	
	
	// TYPES    ---------------------------------
	
	/**
	  * Parameter provided in organization session authorization
	  * (session + membership id + DB connection)
	  */
	type OrganizationParams = (Token, Int, Connection)
	/**
	  * Parameters provided in session authorization (session + DB connection)
	  */
	type SessionParams = (Token, Connection)
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Creates a new authorized request context
	  * @param request Request wrapped by this context
	  * @param resultParser Parser that determines what server responses should look like. Default =
	  *                     use simple json bodies and http statuses.
	  * @param serverSettings Applied server settings (implicit)
	  * @param jsonParser Json parser used for interpreting request json content (implicit)
	  * @return A new request context
	  */
	def apply(request: Request, resultParser: ResultParser = UseRawJson)
	         (implicit serverSettings: ServerSettings, jsonParser: JsonParser): AuthorizedContext =
		new AuthorizedContextImplementation(request, resultParser)
	
	
	// NESTED   ---------------------------
	
	private class AuthorizedContextImplementation(override val request: Request,
	                                              override val resultParser: ResultParser = UseRawJson)
	                                             (implicit override val settings: ServerSettings,
	                                              override val jsonParser: JsonParser)
		extends AuthorizedContext
	{
		override def close() = ()
	}
}

/**
  * This context variation checks user authorization (when required)
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1.0
  */
abstract class AuthorizedContext extends PostContext
{
	// COMPUTED	----------------------------
	
	/**
	  * @return Whether the request in this context contains a bearer token authorization
	  */
	def isTokenAuthorized = request.headers.containsBearerAuthorization
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Languages that were requested in the Accept-Language header (or the X-Accept-Language-Ids -header).
	  *         The languages are listed from most to least preferred. May be empty.
	  */
	def requestedLanguages(implicit connection: Connection) = {
		val acceptedLanguageIds = request.headers.commaSeparatedValues(acceptLanguageIdsHeaderName).flatMap { _.int }
		if (acceptedLanguageIds.nonEmpty) {
			val languageById = DbLanguages(acceptedLanguageIds.toSet).pull.map { l => l.id -> l }.toMap
			// Returns languages in the same order as in the request headers
			acceptedLanguageIds.flatMap { id => languageById.get(id) }
		}
		else {
			val acceptedLanguages = request.headers.acceptedLanguages
				.map { case (code, weight) => code.toLowerCase -> weight }
			if (acceptedLanguages.nonEmpty)
			{
				val acceptedCodes = acceptedLanguages.keySet
				// Maps codes to language ids (if present)
				val languages = DbLanguages.withIsoCodes(acceptedCodes)
				// Orders the languages based on assigned weight
				languages.sortBy { l => -acceptedLanguages(l.isoCode.toLowerCase) }
			}
			else
				Empty
		}
	}
	/**
	  * Reads preferred language ids list either from the Accept-Language header(s)
	  * @param connection DB Connection (implicit)
	  * @return Ids of the requested languages in order from most to least preferred.
	  *         Empty if no headers were specified.
	  */
	def requestedLanguageIds(implicit connection: Connection): LanguageIds = {
		// Checks whether X-Accepted-Language-Ids is provided
		val acceptedIds = request.headers.commaSeparatedValues(acceptLanguageIdsHeaderName).flatMap { _.int }
		if (acceptedIds.nonEmpty)
			LanguageIds(acceptedIds)
		else
		{
			// Reads languages list from the headers (if present)
			val languagesFromHeaders = requestedLanguages
			if (languagesFromHeaders.nonEmpty)
				LanguageIds(languagesFromHeaders.map { _.id })
			else
				LanguageIds(Empty)
		}
	}
	
	/**
	  * @return The model style specified in this request (if specified)
	  */
	def modelStyle = request.headers.get("X-Style").flatMap(ModelStyle.findForKey)
		.orElse { request.parameters("style").string.flatMap(ModelStyle.findForKey) }
	
	
	// OTHER	----------------------------
	
	/**
	  * Reads preferred language ids list either from the Accept-Language header or from the user data
	  * @param userId Id of targeted user (call by name)
	  * @param connection DB Connection (implicit)
	  * @return Ids of the requested languages in order from most to least preferred. Empty only if the user doesn't
	  *         exist or has no linked languages
	  */
	def languageIds(userId: => Int)(implicit connection: Connection): LanguageIds = {
		val requested = requestedLanguageIds
		if (requested.nonEmpty)
			requested
		else
			DbUser(userId).languageIds
	}
	
	/**
	  * Performs the provided function if the request has correct basic authorization (email + password)
	  * @param f Function called when request is authorized. Accepts userId + database connection. Produces an http result.
	  * @return Function result or a result indicating that the request was unauthorized. Wrapped as a response.
	  */
	def basicAuthorized(f: (Int, Connection) => Result) = {
		// Authorizes request with basic auth, finding user id
		val result = request.headers.basicAuthorization match {
			case Some((email, password)) =>
				connectionPool.tryWith { implicit connection =>
					tryAuthenticate(email, password) match {
						// Performs the operation on authorized user id
						case Some(userId) => f(userId, connection)
						case None => Result.Failure(Unauthorized, "Invalid email or password")
					}
				}.getOrMap { e =>
					logger(e, "Unexpected failure during request handling")
					Result.Failure(InternalServerError, e.getMessage)
				}
			case None => Result.Failure(Unauthorized, "Please provide a basic auth header with user email and password")
		}
		result.toResponse(this)
	}
	
	/**
	  * Performs the specified function if the user is authorized (using a token),
	  * a member of the specified organization and has access to the specified scope
	  * (where default is organization data read access)
	  * @param organizationId Id of the organization the user is supposed to be a member of
	  * @param scopeId Id of the targeted scope (default = organization data read access)
	  * @param f              Function called when the user is fully authorized.
	  *                       Takes user session, membership id and database
	  *                       connection as parameters. Returns operation result.
	  * @return An http response based either on the function result or authorization failure.
	  */
	def authorizedInOrganization(organizationId: Int, scopeId: Int = ReadOrganizationData.id)
	                            (f: (Token, Int, Connection) => Result) =
	{
		// Validates the token and checks scope
		authorizedForScopeWithId(scopeId) { (token, connection) =>
			implicit val c: Connection = connection
			// Makes sure the user belongs to the target organization
			token.userAccess.flatMap { _.membershipInOrganizationWithId(organizationId).id } match {
				case Some(membershipId) => f(token, membershipId, connection)
				case None => Result.Failure(Unauthorized, "You're not a member of this organization")
			}
		}
	}
	
	/**
	  * Performs the specified function if:<br>
	  * 1) The request can be authorized using a valid token<br>
	  * 2) The token includes the specified scope (where default is organization data write access)<br>
	  * 3) The authorized user is a member of the specified organization and<br>
	  * 4) The user has the right/authorization to perform the specified task within that organization
	  * @param organizationId Id of the targeted organization
	  * @param taskId Id of the task the user is trying to perform
	  * @param scopeId Id of the targeted scope (default = organization data write access)
	  * @param f Function called when the user is fully authorized. Takes user session, membership id and database
	  *          connection as parameters. Returns operation result.
	  * @return An http response based either on the function result or authorization failure (401 or 403).
	  */
	def authorizedForTask(organizationId: Int, taskId: Int, scopeId: Int = OrganizationActions.id)
	                     (f: (Token, Int, Connection) => Result) =
	{
		// Makes sure the user belongs to the organization and that they have a valid session key authorization
		authorizedInOrganization(organizationId, scopeId) { (session, membershipId, connection) =>
			implicit val c: Connection = connection
			// Makes sure the user has a right to perform the required task
			if (DbMembership(membershipId).allowsTaskWithId(taskId))
				f(session, membershipId, connection)
			else
				Result.Failure(Forbidden,
					"You haven't been granted the right to perform this task within this organization")
		}
	}
	
	/**
	  * Searches the bearer token authorization header for a valid token that may access the specified scope
	  * @param scopeId Id of the scope being accessed
	  * @param f A function called if the token was valid. Accepts the matching token and a database connection.
	  * @return Function response if the request was authorized. Failure response otherwise.
	  */
	def authorizedForScopeWithId(scopeId: Int)(f: (Token, Connection) => Result) =
		_tokenAuthorized { _.havingScopeWithId(scopeId)(_) }(f)
	/**
	  * Searches the bearer token authorization header for a valid token that may access the specified scope
	  * @param scope The scope being accessed
	  * @param f A function called if the token was valid. Accepts the matching token and a database connection.
	  * @return Function response if the request was authorized. Failure response otherwise.
	  */
	def authorizedForScope(scope: ScopeIdWrapper)(f: (Token, Connection) => Result) =
		authorizedForScopeWithId(scope.id)(f)
	
	/**
	  * Searches the bearer token authorization header for a valid token that may access the specified scopes
	  * @param scopeIds Ids of the targeted scopes
	  * @param f A function called if the token is valid. Accepts the matching token and a database connection.
	  * @return Function response if the request was authorized. Failure response otherwise.
	  */
	def authorizedForScopesWithIds(scopeIds: Set[Int])(f: (ScopedToken, Connection) => Result) =
		authorizedWithoutScope { (token, connection) =>
			implicit val c: Connection = connection
			val scoped = token.withScopeLinksPulled
			val missingScopes = scopeIds -- scoped.accessibleScopeIds
			if (missingScopes.isEmpty)
				f(scoped, connection)
			else
				Result.Failure(Unauthorized,
					s"You lack access to following scopes: [${missingScopes.toVector.sorted.mkString(", ")}]")
		}
	/**
	  * Searches the bearer token authorization header for a valid token that may access the specified scopes
	  * @param firstScopeId Id of the targeted scope
	  * @param secondScopeId Id of another targeted scope
	  * @param moreScopeIds Ids of additional targeted scopes
	  * @param f A function called if the token is valid. Accepts the matching token and a database connection.
	  * @return Function response if the request was authorized. Failure response otherwise.
	  */
	def authorizedForScopesWithIds(firstScopeId: Int, secondScopeId: Int, moreScopeIds: Int*)
	                              (f: (ScopedToken, Connection) => Result): Response =
		authorizedForScopesWithIds(moreScopeIds.toSet + firstScopeId + secondScopeId)(f)
	/**
	  * Searches the bearer token authorization header for a valid token that may access the specified scopes
	  * @param firstScope A targeted scope
	  * @param secondScope Another targeted scope
	  * @param moreScopes Additional targeted scopes
	  * @param f A function called if the token is valid. Accepts the matching token and a database connection.
	  * @return Function response if the request was authorized. Failure response otherwise.
	  */
	def authorizedForScopes(firstScope: ScopeIdWrapper, secondScope: ScopeIdWrapper, moreScopes: ScopeIdWrapper*)
	                       (f: (ScopedToken, Connection) => Result): Response =
		authorizedForScopesWithIds((Pair(firstScope, secondScope) ++ moreScopes).map { _.id }.toSet)(f)
	
	/**
	  * Searches the bearer token authorization header for a valid token
	  * @param f A function called if the token was valid. Accepts the matching token and a database connection.
	  * @return Function response if the request was authorized. Failure response otherwise.
	  */
	def authorizedWithoutScope(f: (Token, Connection) => Result) = _tokenAuthorized { _.pull(_) }(f)
	
	/**
	  * Authorizes a request using bearer token authorization
	  * @param tokenTypeName Name used for the token (Eg. 'api key') (Used in failure messages)
	  * @param testToken A function for testing token validity. Accepts the provided token and a database connection.
	  *                Returns a valid item associated with the key (if present)
	  * @param f A function that performs the operation when authentication succeeds. Accepts 1) the item associated
	  *          with the provided token and 2) a database connection and produces a response for the client.
	  * @tparam K Type of item associated with the token
	  * @return Response containing either function <i>f</i> result or an authentication failure
	  *         (if <i>testKey</i> returned None or the token was missing)
	  */
	def tokenAuthorized[K](tokenTypeName: => String)(testToken: (String, Connection) => Option[K])
	                      (f: (K, Connection) => Result) =
	{
		// Checks the token from the bearer token authorization header
		val result = NotEmpty(request.headers.bearerAuthorization) match {
			case Some(token) =>
				// Validates the device token against database
				connectionPool.tryWith { connection =>
					testToken(token, connection) match {
						case Some(authorizedToken) => f(authorizedToken, connection)
						case None => Result.Failure(Unauthorized, s"Invalid or expired $tokenTypeName")
					}
				}.getOrMap { e =>
					logger(e, "Unexpected failure during request handling")
					Result.Failure(InternalServerError, e.getMessage)
				}
			case None => Result.Failure(Unauthorized, s"Please provided a bearer auth hearer with a $tokenTypeName")
		}
		result.toResponse(this)
	}
	
	private def _tokenAuthorized(pullToken: (DbTokenMatch, Connection) => Option[Token])
	                            (f: (Token, Connection) => Result) = {
		tokenAuthorized("token") { (token, connection) =>
			pullToken(DbToken.matching(token), connection)
		} { (token, connection) =>
			val result = f(token, connection)
			// Closes a single-use token on success
			if (token.isSingleUseOnly && result.isSuccess) {
				implicit val c: Connection = connection
				token.access.deprecate()
			}
			result
		}
	}
	
	// Finds user id and checks the password
	private def tryAuthenticate(email: String, password: String)(implicit connection: Connection) =
		DbUserSettings.withEmail(email).userId.filter { userId => DbUserPassword.ofUserWithId(userId).test(password) }
}