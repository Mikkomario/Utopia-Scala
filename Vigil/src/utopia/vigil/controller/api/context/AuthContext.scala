package utopia.vigil.controller.api.context

import utopia.access.model.enumeration.Status.Unauthorized
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.database.VigilContext
import utopia.vigil.database.access.token.AccessToken
import utopia.vigil.database.value.ScopeTarget
import utopia.vigil.model.cached.token.TokenIdRefs

/**
 * Common trait for request contexts that implement Vigil-based authentication
 * @tparam A Type of the wrapped request body format
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
trait AuthContext[+A] extends RequestContext[A]
{
	def authorized(requiredScopes: Iterable[ScopeTarget])(f: (TokenIdRefs, Connection) => RequestResult) =
		headers.bearerAuthorization.ifNotEmpty match {
			case Some(bearerToken) =>
				VigilContext.connectionPool
					.tryWith { implicit connection =>
						AccessToken.idRefs.active.withHash(bearerToken).pull match {
							case Some(tokenIdRefs) => ???
							case None => Unauthorized -> "Invalid or expired authorization token"
						}
					}
					.getOrMap { error =>
						// TODO: Log the error and yield 500
						???
					}
			case None => Unauthorized -> "Please specify the `Authorization:Bearer ...` header"
		}
}
