package utopia.exodus.rest.resource

import utopia.exodus.rest.resource.description.{DescriptionRolesNode, LanguageFamiliaritiesNode, LanguagesNode, RolesNode, TasksNode}
import utopia.exodus.rest.resource.device.DevicesNode
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
	  * All publicly available resources
	  */
	val public = Vector(DescriptionRolesNode, LanguagesNode, LanguageFamiliaritiesNode, RolesNode, TasksNode)
	
	/**
	  * All resources that require authorization for at least some features
	  */
	val authorized = Vector(DevicesNode, OrganizationsNode, UsersNode)
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * All root level resources introduced in the Exodus project
	  */
	def all = public ++ authorized
}
