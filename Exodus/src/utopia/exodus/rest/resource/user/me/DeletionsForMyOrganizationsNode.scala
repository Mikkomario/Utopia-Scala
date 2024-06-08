package utopia.exodus.rest.resource.user.me

import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.collection.immutable.Single
import utopia.nexus.rest.{NotImplementedResource, ResourceWithChildren}

/**
 * Used for accessing deletions concerning the organizations the current user is a member of
 * @author Mikko Hilpinen
 * @since 16.5.2020, v1
 */
object DeletionsForMyOrganizationsNode
	extends ResourceWithChildren[AuthorizedContext] with NotImplementedResource[AuthorizedContext]
{
	override val name = "deletions"
	override val children = Single(PendingDeletionsForMyOrganizationsNode)
}
