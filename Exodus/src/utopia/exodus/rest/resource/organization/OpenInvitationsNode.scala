package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Get
import utopia.exodus.rest.resource.user.me.MyInvitationsNode
import utopia.exodus.rest.resource.user.me.MyInvitationsNode.MyInvitationResponsesNode
import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.http.Path
import utopia.nexus.rest.{Resource, ResourceWithChildren}

/**
  * Used for answering an open organization invitation using an email validation token.
  * Intended to appear as: invitations/open
  * @author Mikko Hilpinen
  * @since 25.11.2021, v3.1
  */
@deprecated("Replaced with new implementation of users/me/invitations and POST users", "v4.0")
object OpenInvitationsNode extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES  -----------------------------
	
	override val name = "open"
	override val allowedMethods = Vector(Get)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def children =
		Vector[Resource[AuthorizedContext]](MyInvitationResponsesNode)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		MyInvitationsNode.toResponse(remainingPath)
}
