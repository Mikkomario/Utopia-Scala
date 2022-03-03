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
	
	/**
	  * All publicly available resources (may be customized)
	  */
	@deprecated("Please use .all instead", "v4.0")
	val publicDescriptions = Vector(DescriptionRolesNode, LanguagesNode, LanguageFamiliaritiesNode)
	/**
	 * Resources that are available publicly or with some other customizable authorization
	 */
	@deprecated("Please use .all instead", "v4.0")
	val customAuthorized = Vector(UsersNode)
	/**
	  * All resources that require authorization for at least some features
	  */
	@deprecated("Please use .all instead", "v4.0")
	val authorized = Vector(OrganizationsNode, RolesNode, TasksNode)
	
	
	// COMPUTED	---------------------------------
	
	/**
	 * @return All Exodus resources. Resources that are not behind a session authorization don't use any authorization,
	 *         which may be a security risk. Consider using apply(Boolean) instead or creating a custom set of
	 *         resources with publicDescriptions, customAuthorized and authorized
	 */
	@deprecated("Please use .all instead", "v4.0")
	def default = all
	
	/**
	 * @param useApiKey Whether api key authorization should be used for resources that would otherwise be
	 *                  publicly available
	 * @return All Exodus resources
	 */
	@deprecated("Please use .all instead", "v4.0")
	def apply(useApiKey: Boolean) = all
}
