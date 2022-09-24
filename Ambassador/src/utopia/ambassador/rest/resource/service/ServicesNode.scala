package utopia.ambassador.rest.resource.service

import utopia.access.http.Method.Get
import utopia.ambassador.controller.implementation.AcquireTokens
import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.database.access.many.service.DbAuthServices
import utopia.ambassador.rest.util.ServiceTarget
import utopia.exodus.model.enumeration.ExodusScope.ReadGeneralData
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.collection.template.MapLike
import utopia.flow.generic.casting.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Follow
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Facilitates access to multiple services
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  * @param tokenAcquirer An interface for acquiring new access tokens
  * @param redirectors Cache for acquiring redirector implementations. Takes service ids.
  * @param name Name of this node (default = "services")
  */
class ServicesNode(tokenAcquirer: AcquireTokens, redirectors: MapLike[Int, AuthRedirector],
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
		Follow(new ServiceNode(target, tokenAcquirer, redirectors), path.tail)
	}
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForScope(ReadGeneralData) { (_, connection) =>
			implicit val c: Connection = connection
			// Returns simple service data
			Result.Success(DbAuthServices.pull.map { _.toModel })
		}
	}
}
