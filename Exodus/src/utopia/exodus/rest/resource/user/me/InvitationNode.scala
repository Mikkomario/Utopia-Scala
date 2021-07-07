package utopia.exodus.rest.resource.user.me

import utopia.access.http.Status.NotImplemented
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.util.StringExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result

/**
 * A rest resource for accessing individual invitation's data
 * @author Mikko Hilpinen
 * @since 6.5.2020, v1
 */
case class InvitationNode(invitationId: Int) extends Resource[AuthorizedContext]
{
	override def name = invitationId.toString
	
	override val allowedMethods = Vector()
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) = Result.Failure(
		NotImplemented, "Invitation data access hasn't been implemented yet").toResponse
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "response")
			Follow(InvitationResponseNode(invitationId), path.tail)
		else
			Error(message = Some("Currently invitation only has 'response' sub-resource"))
	}
}
