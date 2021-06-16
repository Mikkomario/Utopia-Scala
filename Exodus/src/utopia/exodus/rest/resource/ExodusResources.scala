package utopia.exodus.rest.resource

import utopia.exodus.rest.resource.description.{DescriptionRolesNode, LanguageFamiliaritiesNode, LanguagesNode, RolesNode, TasksNode}
import utopia.exodus.rest.resource.device.DevicesNode
import utopia.exodus.rest.resource.organization.OrganizationsNode
import utopia.exodus.rest.resource.user.{QuestSessionsNode, UsersNode}

/**
  * Lists all resources introduced in the Exodus project
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
object ExodusResources
{
	// ATTRIBUTES	------------------------------
	
	/**
	  * All publicly available resources (may be customized)
	  */
	val publicDescriptions = Vector(DescriptionRolesNode, LanguagesNode, LanguageFamiliaritiesNode)
	/**
	 * Resources that are available publicly or with some other customizable authorization
	 */
	val customAuthorized = Vector(UsersNode)
	/**
	  * All resources that require authorization for at least some features
	  */
	val authorized = Vector(DevicesNode, OrganizationsNode, RolesNode, TasksNode, QuestSessionsNode)
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * All root level resources introduced in the Exodus project
	  */
	@deprecated("Please use .default instead", "v1.0.1")
	def all = default
	/**
	 * @return All Exodus resources. Resources that are not behind a session authorization don't use any authorization,
	 *         which may be a security risk. Consider using apply(Boolean) instead or creating a custom set of
	 *         resources with publicDescriptions, customAuthorized and authorized
	 */
	def default = apply(useApiKey = false)
	
	/**
	 * @param useApiKey Whether api key authorization should be used for resources that would otherwise be
	 *                  publicly available
	 * @return All Exodus resources
	 */
	def apply(useApiKey: Boolean) =
		publicDescriptions.map { factory => if (useApiKey) factory.forApiKey else factory.public } ++
			customAuthorized.map { factory => if (useApiKey) factory.forApiKey else factory.public } ++ authorized
}
