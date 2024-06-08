package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Get
import utopia.citadel.database.access.single.organization.DbOrganization
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * An access points to various users in the described organization
  * @author Mikko Hilpinen
  * @since 11.5.2020, v1
  */
case class OrganizationMembersNode(organizationId: Int) extends Resource[AuthorizedContext]
{
	override val name = "users"
	override val allowedMethods = Single(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// User must be a member of the organization to see data from other members
		context.authorizedInOrganization(organizationId) { (session, _, connection) =>
			implicit val c: Connection = connection
			// Retrieves all memberships, associated task ids and user settings
			// Omits the requesting user
			val baseAccess = DbOrganization(organizationId).membershipsWithRoles
			val memberships = (session.ownerId match {
				case Some(userId) => baseAccess.notOfUserWithId(userId)
				case None => baseAccess
			}).detailed
			// Produces a response based on the read data
			// Supports styling options
			val style = session.modelStyle
			Result.Success(memberships.map { _.toModelWith(style) }.toVector)
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "me")
			Follow(MemberNode(organizationId, None), path.tail)
		else
			path.head.int match
			{
				case Some(userId) => Follow(MemberNode(organizationId, Some(userId)), path.tail)
				case None => Error(message = s"${path.head} is not a valid user id")
			}
	}
}
