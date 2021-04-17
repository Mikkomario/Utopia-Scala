package utopia.exodus.rest.resource.description

import utopia.exodus.database.access.many.DbDescriptions
import utopia.exodus.database.factory.description.DescriptionRoleFactory
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.stored.description.{DescriptionLink, DescriptionRole}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object DescriptionRolesNode extends PublicDescriptionsNodeFactory[DescriptionRolesNode]
{
	override def apply(authorization: (AuthorizedContext, => Result, Connection) => Result) =
		new DescriptionRolesNode(authorization)
}

/**
  * Used for retrieving descriptions of all description roles
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  * @param authorization A function for authorizing incoming requests. Accepts
  */
class DescriptionRolesNode(authorization: (AuthorizedContext, => Result, Connection) => Result)
	extends PublicDescriptionsNode[DescriptionRole, DescribedDescriptionRole]
{
	// IMPLEMENTED	------------------------
	
	override val name = "description-roles"
	
	override protected def authorize(onAuthorized: => Result)
									(implicit context: AuthorizedContext, connection: Connection) =
		authorization(context, onAuthorized, connection)
	
	override protected def items(implicit connection: Connection) = DescriptionRoleFactory.getAll()
	
	override protected def descriptionsAccess = DbDescriptions.ofAllDescriptionRoles
	
	override protected def idOf(item: DescriptionRole) = item.id
	
	override protected def combine(item: DescriptionRole, descriptions: Set[DescriptionLink]) =
		DescribedDescriptionRole(item, descriptions)
}
