package utopia.exodus.rest.resource.organization

import utopia.exodus.rest.resource.NotImplementedResource
import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.rest.ResourceWithChildren

/**
  * Used for interacting with organization invitations
  * @author Mikko Hilpinen
  * @since 25.11.2021, v3.1
  */
object InvitationsNode extends ResourceWithChildren[AuthorizedContext] with NotImplementedResource[AuthorizedContext]
{
	override val name = "invitations"
	override val children = Vector(OpenInvitationsNode)
}
