package utopia.exodus.rest.resource.organization

import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.rest.{NotImplementedResource, ResourceWithChildren}

/**
  * Used for interacting with organization invitations
  * @author Mikko Hilpinen
  * @since 25.11.2021, v3.1
  */
@deprecated("Replaced with new implementation of users/me/invitations", "v4.0")
object InvitationsNode extends ResourceWithChildren[AuthorizedContext] with NotImplementedResource[AuthorizedContext]
{
	override val name = "invitations"
	override val children = Vector(OpenInvitationsNode)
}
