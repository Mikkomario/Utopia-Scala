package utopia.vigil.controller.api.context

import utopia.access.model.enumeration.Status.{InternalServerError, Unauthorized}
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.{RequestResult, ResponseContent}
import utopia.vault.database.Connection
import utopia.vigil.database.VigilContext
import utopia.vigil.database.access.token.AccessToken
import utopia.vigil.database.access.token.scope.AccessTokenScopes
import utopia.vigil.model.cached.scope.{ScopeTarget, Scopes}
import utopia.vigil.model.cached.token.TokenIdRefs

/**
 * Common trait for request contexts that implement Vigil-based authentication
 * @tparam A Type of the wrapped request body format
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
trait AuthContext[+A] extends RequestContext[A]
{
	/**
	 * Verifies that the request is authorized
	 * @param f A function called if the request is properly authorized.
	 *          Receives 2 parameters:
	 *          1. IDs of the used auth token
	 *          1. DB connection
	 *
	 *          Returns the result to send back to the client
	 *
	 * @return One of the following results:
	 *         - If the request was authorized, the result of 'f'
	 *         - If 'f' threw, 500
	 *         - If the request was not authorized, 401
	 */
	def authorized(f: (TokenIdRefs, Connection) => RequestResult): RequestResult = authorizedFor(Scopes.empty)(f)
	
	/**
	 * Verifies that the request is authorized in a specific auth scope
	 * @param scope Scope that must be accessible in order the request to complete
	 * @param f A function called if the request is properly authorized.
	 *          Receives 2 parameters:
	 *          1. IDs of the used auth token
	 *          1. DB connection
	 *
	 *          Returns the result to send back to the client
	 *
	 * @return One of the following results:
	 *         - If the request was authorized, the result of 'f'
	 *         - If 'f' threw, 500
	 *         - If the request was not authorized, 401
	 */
	def authorizedFor(scope: ScopeTarget)(f: (TokenIdRefs, Connection) => RequestResult): RequestResult =
		authorizedFor(Scopes(scope))(f)
	/**
	 * Verifies that the request is authorized in a specific auth scope
	 * @param firstScope First scope that must be accessible in order the request to complete
	 * @param secondScope Second scope that must be accessible in order the request to complete
	 * @param moreScopes Other scopes that must be accessible in order the request to complete
	 * @param f A function called if the request is properly authorized.
	 *          Receives 2 parameters:
	 *          1. IDs of the used auth token
	 *          1. DB connection
	 *
	 *          Returns the result to send back to the client
	 *
	 * @return One of the following results:
	 *         - If the request was authorized, the result of 'f'
	 *         - If 'f' threw, 500
	 *         - If the request was not authorized, 401
	 */
	def authorizedFor(firstScope: ScopeTarget, secondScope: ScopeTarget, moreScopes: ScopeTarget*)
	                 (f: (TokenIdRefs, Connection) => RequestResult): RequestResult =
		authorizedFor(Pair(firstScope, secondScope) ++ moreScopes)(f)
	/**
	 * Verifies that the request is authorized in a specific auth scope
	 * @param requiredScopes Scopes that must be accessible in order the request to complete
	 * @param f A function called if the request is properly authorized.
	 *          Receives 2 parameters:
	 *          1. IDs of the used auth token
	 *          1. DB connection
	 *
	 *          Returns the result to send back to the client
	 *
	 * @return One of the following results:
	 *         - If the request was authorized, the result of 'f'
	 *         - If 'f' threw, 500
	 *         - If the request was not authorized, 401
	 */
	def authorizedFor(requiredScopes: Scopes)(f: (TokenIdRefs, Connection) => RequestResult): RequestResult =
		// Checks the authorization token
		headers.bearerAuthorization.ifNotEmpty match {
			case Some(bearerToken) =>
				VigilContext.connectionPool
					.tryWith[RequestResult] { implicit connection =>
						AccessToken.idRefs.active.withKey(bearerToken).pull match {
							// Case: Valid token => Checks the scope (if needed)
							case Some(token) =>
								implicit val _token: TokenIdRefs = token
								requireScopes(requiredScopes) { f(token, connection) }
								
							// Case: Invalid or expired auth token => 401
							case None => Unauthorized -> "Invalid or expired authorization token"
						}
					}
					// Catches all exceptions and returns 500 if one is encountered
					.getOrMap { error =>
						VigilContext.log(error, "Unexpected error while handling a request", Model.from(
							"method" -> request.method.name, "path" -> request.path))
						InternalServerError ->
							"The server encountered an unexpected failure and could not complete this request"
					}
			// Case: No auth token specified => 401
			case None => Unauthorized -> "Please specify the `Authorization:Bearer ...` header"
		}
	
	/**
	 * Makes sure the request has access to a specific scope
	 * @param scope Scope that the client must have access to
	 * @param result Result to yield on sufficient authorization (call-by-name)
	 * @param connection Implicit DB connection
	 * @param token Implicit auth token used
	 * @return 'result', or a 401 request failure if the authorization was lacking
	 */
	def requireScope(scope: ScopeTarget)(result: => RequestResult)
	                (implicit connection: Connection, token: TokenIdRefs): RequestResult =
		requireScopes(Scopes(scope))(result)
	/**
	 * Makes sure the request has access to a specific set of scopes
	 * @param scope1 First required scope
	 * @param scope2 Second required scope
	 * @param moreScopes Other required scopes
	 * @param result Result to yield on sufficient authorization (call-by-name)
	 * @param connection Implicit DB connection
	 * @param token Implicit auth token used
	 * @return 'result', or a 401 request failure if the authorization was lacking
	 */
	def requireScopes(scope1: ScopeTarget, scope2: ScopeTarget, moreScopes: ScopeTarget*)(result: => RequestResult)
	                 (implicit connection: Connection, token: TokenIdRefs): RequestResult =
		requireScopes(Pair(scope1, scope2) ++ moreScopes)(result)
	/**
	 * Makes sure the request has access to a specific set of scopes
	 * @param scopes Scopes that the client must have access to
	 * @param result Result to yield on sufficient authorization (call-by-name)
	 * @param connection Implicit DB connection
	 * @param token Implicit auth token used
	 * @return 'result', or a 401 request failure if the authorization was lacking
	 */
	def requireScopes(scopes: Scopes)(result: => RequestResult)
	                 (implicit connection: Connection, token: TokenIdRefs): RequestResult =
		testScopes(scopes).getOrElse(result)
	
	/**
	 * Checks whether the request has access to a specific auth scope
	 * @param scope Scope to test
	 * @param connection Implicit DB connection
	 * @param token Implicit auth token used
	 * @return If the authorization token didn't have proper access scope, yields a failure result.
	 *         If authorization was successful, yields None.
	 */
	def testScope(scope: ScopeTarget)(implicit connection: Connection, token: TokenIdRefs): Option[RequestResult] =
		testScopes(Scopes(scope))
	/**
	 * Checks whether the request has access to a specific set of scopes
	 * @param scope1 The first tested scope
	 * @param scope2 The second tested scope
	 * @param moreScopes Other scopes to test
	 * @param connection Implicit DB connection
	 * @param token Implicit auth token used
	 * @return If the authorization token didn't have proper access scope, yields a failure result.
	 *         If authorization was successful, yields None.
	 */
	def testScopes(scope1: ScopeTarget, scope2: ScopeTarget, moreScopes: ScopeTarget*)
	              (implicit connection: Connection, token: TokenIdRefs): Option[RequestResult] =
		testScopes(Pair(scope1, scope2) ++ moreScopes)
	/**
	 * Checks whether the request has access to a specific set of scopes
	 * @param scopes Scopes to test
	 * @param connection Implicit DB connection
	 * @param token Implicit auth token used
	 * @return If the authorization token didn't have proper access scope, yields a failure result.
	 *         If authorization was successful, yields None.
	 */
	def testScopes(scopes: Scopes)(implicit connection: Connection, token: TokenIdRefs) =
	{
		// Case: No scope is required => Calls the specified function
		if (scopes.isEmpty)
			None
		// Case: Certain scopes are required => Makes sure the auth token has those
		else {
			val accessibleScopeIds = AccessTokenScopes.ofToken(token.id).usable.scopeIds.toSet
			scopes.notContainedWithin(accessibleScopeIds).notEmpty.map { missingScopes =>
				RequestResult(ResponseContent(Model.from("missingScopes" -> missingScopes.toValue),
					"Your authentication token lacks the sufficient authorization scopes"))
			}
		}
	}
}
