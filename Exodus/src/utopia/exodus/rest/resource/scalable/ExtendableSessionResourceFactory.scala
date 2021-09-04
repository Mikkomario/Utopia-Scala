package utopia.exodus.rest.resource.scalable

import utopia.access.http.Method
import utopia.exodus.model.stored.UserSession
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.rest.util.AuthorizedContext.SessionParams
import utopia.flow.datastructure.immutable.Lazy
import utopia.nexus.http.Path
import utopia.nexus.rest.scalable.{ExtendableResource, ExtendableResourceFactory}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * A common class for resource factories that produce session-authorized resources and support extensions
 * @author Mikko Hilpinen
 * @since 19.7.2021 v2.0.1
  * @tparam A Type of parameter accepted when creating new rest nodes (E.g. item id or next path part)
  * @tparam R Type of rest nodes produced by this factory (must support extensions)
 */
abstract class ExtendableSessionResourceFactory[A, +R <: ExtendableResource[AuthorizedContext, SessionParams]]
	extends ExtendableResourceFactory[A, AuthorizedContext, SessionParams, R]
{
	/**
	 * Adds a new use case to all resources that will be created through this factory
	 * @param method Method expected in the use case
	 * @param useCase A function that accepts 1) resource creation parameter, 2) user session,
	 *                3) database connection, 4) request context, 5) remaining request path and
	 *                6) default implementation (lazy) and returns a result
	 */
	def addUseCase(method: Method)(useCase: (A, UserSession, Connection, AuthorizedContext, Option[Path],
		Lazy[Result]) => Result): Unit =
		addUseCase(SessionUseCaseImplementation
			.factory[A](method) { (organizationId, session, connection, context, path, default) =>
				useCase(organizationId, session, connection, context, path, default)
			})
}
