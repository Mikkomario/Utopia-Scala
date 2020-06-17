package utopia.exodus.rest.resource.organization

import utopia.access.http.Status.NotImplemented
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result

/**
  * An access points to various users in the described organization
  * @author Mikko Hilpinen
  * @since 11.5.2020, v2
  */
case class OrganizationMembersNode(organizationId: Int) extends Resource[AuthorizedContext]
{
	override val name = "users"
	
	override val allowedMethods = Vector()
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) = Result.Failure(
		NotImplemented, "Organization members resource doesn't support any methods yet").toResponse
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "me")
			Follow(MemberNode(organizationId, None), path.tail)
		else
			path.head.int match
			{
				case Some(userId) => Follow(MemberNode(organizationId, Some(userId)), path.tail)
				case None => Error(message = Some(s"${path.head} is not a valid user id"))
			}
	}
}
