package utopia.exodus.rest.resource

import utopia.exodus.rest.resource.description.{DescriptionRolesNode, LanguageFamiliaritiesNode, LanguagesNode, RolesNode, TasksNode}
import utopia.exodus.rest.resource.email.EmailsNode
import utopia.exodus.rest.resource.organization.OrganizationsNode
import utopia.exodus.rest.resource.user.UsersNode

/**
  * Lists all resources introduced in the Exodus project
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
object ExodusResources
{
	// ATTRIBUTES	------------------------------
	
	/**
	  * All root rest nodes introduced in Exodus
	  */
	val all = Vector(DescriptionRolesNode, LanguagesNode, LanguageFamiliaritiesNode, RolesNode, TasksNode,
		UsersNode, OrganizationsNode, EmailsNode)
}
