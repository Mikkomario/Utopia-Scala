package utopia.exodus.rest.resource.user.me

import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.rest.{NotImplementedResource, ResourceWithChildren}

/**
 * A rest resource for accessing individual invitation's data
 * @author Mikko Hilpinen
 * @since 6.5.2020, v1
 */
case class InvitationNode(invitationId: Int)
	extends ResourceWithChildren[AuthorizedContext] with NotImplementedResource[AuthorizedContext]
{
	override def name = invitationId.toString
	override def children = Vector(InvitationResponseNode(invitationId))
}
