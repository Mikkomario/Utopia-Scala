package utopia.ambassador.rest.resource.service

import utopia.access.http.Method.Get
import utopia.ambassador.controller.implementation.AcquireTokens
import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.database.access.many.service.DbAuthServices
import utopia.ambassador.rest.util.ServiceTarget
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.caching.multi.CacheLike
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Follow
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Facilitates access to multiple services
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  * @param tokenAcquirers Cache for acquiring tools for accessing new tokens
  *                       (takes service id and returns an token acquisition implementation)
  * @param redirectors Cache for acquiring redirector implementations. Takes service ids.
  * @param name Name of this node (default = "services")
  */
class ServicesNode(tokenAcquirers: CacheLike[Int, AcquireTokens], redirectors: CacheLike[Int, AuthRedirector],
                   override val name: String = "services")
	extends Resource[AuthorizedContext]
{
	override def allowedMethods = Vector(Get)
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		// Checks whether the next path part is an integer or a string and converts it to a service target
		val target = path.head.int match
		{
			case Some(serviceId) => ServiceTarget.id(serviceId)
			case None => ServiceTarget.name(path.head)
		}
		Follow(new ServiceNode(target, tokenAcquirers, redirectors), path.tail)
	}
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (_, connection) =>
			implicit val c: Connection = connection
			// Returns simple service data
			Result.Success(DbAuthServices.pull.map { _.toModel })
		}
	}
}
