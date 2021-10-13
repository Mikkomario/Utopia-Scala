package utopia.citadel.database.access.many.description

import utopia.vault.model.immutable.Table

/**
  * Used for accessing various types of descriptions
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1.0
  */
@deprecated("This object will be replaced with a description access point with the same name", "v1.3")
object DbDescriptions
{
	// COMPUTED	----------------------------
	
	/**
	  * An access point to all description role descriptions
	  */
	@deprecated("Please use DbDescriptionRoleDescriptionsInstead", "v1.3")
	def ofAllDescriptionRoles = DbDescriptionRoleDescriptions
	/**
	  * An access point to all language descriptions
	  */
	@deprecated("Please use DbLanguageDescriptions", "v1.3")
	def ofAllLanguages = DbLanguageDescriptions
	/**
	  * An access point to all language familiarity description
	  */
	@deprecated("Please use DbLanguageFamiliarityDescriptions instead", "v1.3")
	def ofAllLanguageFamiliarities = DbLanguageFamiliarityDescriptions
	/**
	  * An access point to all role descriptions
	  */
	@deprecated("Please use DbUserRoleDescriptions instead", "v1.3")
	def ofAllUserRoles = DbUserRoleDescriptions
	/**
	  * An access point to all task descriptions
	  */
	@deprecated("Please use DbTaskDescriptions instead", "v1.3")
	def ofAllTasks = DbTaskDescriptions
	/**
	  * An access point to all organization descriptions
	  */
	@deprecated("Please use DbOrganizationDescriptions instead", "v1.3")
	def ofAllOrganizations = DbOrganizationDescriptions
	/**
	  * An access point to all device descriptions
	  */
	@deprecated("Please use DbDeviceDescriptions instead", "v1.3")
	def ofAllDevices = DbDeviceDescriptions
	
	
	// OTHER	----------------------------
	
	/**
	  * @param organizationId Organization id
	  * @return An access point to that organization's descriptions
	  */
	@deprecated("Please use DbOrganizationDescriptions instead", "v1.3")
	def ofOrganizationWithId(organizationId: Int) = DbOrganizationDescriptions(organizationId)
	/**
	  * @param organizationIds Organization ids
	  * @return An access point to descriptions of those organizations
	  */
	@deprecated("Please use DbOrganizationDescriptions instead", "v1.3")
	def ofOrganizationsWithIds(organizationIds: Set[Int]) =
		DbOrganizationDescriptions(organizationIds)
	
	/**
	  * @param deviceId Device id
	  * @return An access point to that device's descriptions
	  */
	@deprecated("Please use DbDeviceDescriptions instead", "v1.3")
	def ofDeviceWithId(deviceId: Int) = DbDeviceDescriptions(deviceId)
	/**
	  * @param deviceIds Device ids
	  * @return An access point to descriptions of those devices
	  */
	@deprecated("Please use DbDeviceDescriptions instead", "v1.3")
	def ofDevicesWithIds(deviceIds: Set[Int]) = DbDeviceDescriptions(deviceIds)
	
	/**
	  * @param taskId Task id
	  * @return An access point to descriptions of that task type
	  */
	@deprecated("Please use DbTaskDescriptions instead", "v1.3")
	def ofTaskWithId(taskId: Int) = DbTaskDescriptions(taskId)
	/**
	  * @param taskIds Ids of targeted tasks
	  * @return An access point to descriptions of those task types
	  */
	@deprecated("Please use DbTaskDescriptions instead", "v1.3")
	def ofTasksWithIds(taskIds: Set[Int]) = DbTaskDescriptions(taskIds)
	
	/**
	  * @param roleId User role id
	  * @return An access point to descriptions of that user role
	  */
	@deprecated("Please use DbUserRoleDescriptions instead", "v1.3")
	def ofUserRoleWithId(roleId: Int) = DbUserRoleDescriptions(roleId)
	/**
	  * @param roleIds Ids of targeted user roles
	  * @return An access point to descriptions of those roles
	  */
	@deprecated("Please use DbUserRoleDescriptions instead", "v1.3")
	def ofUserRolesWithIds(roleIds: Set[Int]) = DbUserRoleDescriptions(roleIds)
	
	/**
	  * @param languageId Language id
	  * @return An access point to that language's descriptions
	  */
	@deprecated("Please use DbLanguageDescriptions", "v1.3")
	def ofLanguageWithId(languageId: Int) = DbLanguageDescriptions(languageId)
	/**
	  * @param languageIds Language ids
	  * @return An access point to descriptions of languages with those ids
	  */
	@deprecated("Please use DbLanguageDescriptions", "v1.3")
	def ofLanguagesWithIds(languageIds: Set[Int]) =
		DbLanguageDescriptions(languageIds)
	
	/**
	  * @param familiarityId Language familiarity id
	  * @return An access point to that familiarity's descriptions
	  */
	@deprecated("Please use DbLanguageFamiliarityDescriptions instead", "v1.3")
	def ofLanguageFamiliarityWithId(familiarityId: Int) =
		DbLanguageFamiliarityDescriptions(familiarityId)
	/**
	  * @param familiarityIds A set of language familiarity ids
	  * @return An access point to those familiarities descriptions
	  */
	@deprecated("Please use DbLanguageFamiliarityDescriptions instead", "v1.3")
	def ofLanguageFamiliaritiesWithIds(familiarityIds: Set[Int]) =
		DbLanguageFamiliarityDescriptions(familiarityIds)
	
	/**
	  * Creates a new access point to a custom description table + link column combination
	  * @param linkTable Table that contains targeted description links
	  * @param linkAttName Name of the property that contains links to the described items
	  * @return A new descriptions access point
	  */
	@deprecated("Please use DbDescriptionLinksAccess instead", "v1.3")
	def access(linkTable: Table, linkAttName: String) =
		DescriptionLinksAccess(linkTable, linkAttName)
}
