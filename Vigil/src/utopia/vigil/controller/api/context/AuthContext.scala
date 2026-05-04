package utopia.vigil.controller.api.context

import utopia.access.model.enumeration.Status.{InternalServerError, Unauthorized}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.{RequestResult, ResponseContent}
import utopia.vault.database.Connection
import utopia.vigil.database.VigilContext
import utopia.vigil.database.access.token.AccessToken
import utopia.vigil.database.access.token.scope.AccessTokenScopes
import utopia.vigil.model.cached.scope.ScopeTarget
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
	def authorized(f: (TokenIdRefs, Connection) => RequestResult): RequestResult = authorizedFor(Empty)(f)
	
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
		authorizedFor(Single(scope))(f)
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
	def authorizedFor(requiredScopes: Iterable[ScopeTarget])
	                 (f: (TokenIdRefs, Connection) => RequestResult): RequestResult =
		// Checks the authorization token
		headers.bearerAuthorization.ifNotEmpty match {
			case Some(bearerToken) =>
				VigilContext.connectionPool
					.tryWith[RequestResult] { implicit connection =>
						AccessToken.idRefs.active.withHash(bearerToken).pull match {
							// Case: Valid token => Checks the scope (if needed)
							case Some(token) =>
								// Case: No scope is required => Calls the specified function
								if (requiredScopes.isEmpty)
									f(token, connection)
								// Case: Certain scopes are required => Makes sure the auth token has those
								else {
									val accessibleScopeIds = AccessTokenScopes.ofToken(token.id).usable.scopeIds.toSet
									requiredScopes.filterNot { _.isContainedWithin(accessibleScopeIds) }
										.notEmpty match
									{
										// Case: Some required scopes are missing => 401
										case Some(missingScopes) =>
											RequestResult(ResponseContent(Model.from(
												"missingScopes" -> missingScopes.view.map { _.key }.toOptimizedSeq),
												"Your authentication token lacks the sufficient authorization scopes"))
										
										// Case: All required scopes are accessible => Calls the specified function
										case None => f(token, connection)
									}
								}
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
}
