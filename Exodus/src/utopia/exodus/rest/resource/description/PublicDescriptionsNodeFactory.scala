package utopia.exodus.rest.resource.description

import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A common trait for factory classes for nodes accessible without session authorization
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
trait PublicDescriptionsNodeFactory[+A]
{
	// ABSTRACT	------------------------------
	
	/**
	  * Creates a new node that uses specified authorization mechanism
	  * @param authorization A function that accepts 1) request context, 2) function for producing a result for
	  *                      an authenticated request and 3) A database connection, producing a result for
	  *                      response generation
	  * @return A new node that uses specified authorization function
	  */
	def apply(authorization: (AuthorizedContext, => Result, Connection) => Result): A
	
	
	// COMPUTED	------------------------------
	
	/**
	  * A node implementation which doesn't require any authorization
	  */
	def public = apply((_, f, _) => f)
	
	/**
	  * A node implementation that requires api key authorization
	  */
	def forApiKey = apply((context, f, connection) => context.apiKeyAuthorizedWithConnection { _ => f }(connection))
}
