package utopia.exodus.rest.resource

import utopia.access.http.Status.InternalServerError
import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.http.Response
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * A common trait for rest resource factory classes which allow outside customization of authorization methods.
  * Used with objects that are accessible before a session key can or should be acquired
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  * @tparam A Type of created resource
  */
trait CustomAuthorizationResourceFactory[+A]
{
	// ABSTRACT	------------------------------
	
	/**
	  * Creates a new resource using the specified authorization function
	  * @param authorize An authorization function. Accepts request context and a function that should be called
	  *                  if authorization succeeds. Produces a response (referring to passed function result if
	  *                  that function was called). The passed function accepts an open database connection and
	  *                  produces a request result.
	  * @return A new rest resource using specified authorization function
	  */
	def apply(authorize: (AuthorizedContext, Connection => Result) => Response): A
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return A node that doesn't use additional authorization
	  */
	def public = apply { (context, f) =>
		import utopia.exodus.util.ExodusContext._
		implicit val c: AuthorizedContext = context
		
		connectionPool.tryWith(f) match
		{
			case Success(result) => result.toResponse
			case Failure(error) =>
				handleError(error, s"Unexpected error during handling of request: ${
					context.request.method} ${context.request.targetUrl}")
				Result.Failure(InternalServerError, error.getMessage).toResponse
		}
	}
	
	/**
	  * @return A node that uses api key authorization by default.
	  */
	def forApiKey = apply { (context, onAuthorized) =>
		context.apiKeyAuthorized { (_, connection) => onAuthorized(connection) }
	}
}
