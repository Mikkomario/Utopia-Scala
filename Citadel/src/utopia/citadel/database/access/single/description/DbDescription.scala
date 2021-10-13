package utopia.citadel.database.access.single.description

import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper

@deprecated("This object will be replaced with a description access object", "v1.3")
object DbDescription
{
	/**
	 * @param organizationId Organization id
	 * @return An access point to that organization's individual descriptions
	 */
	@deprecated("Please use DbOrganizationDescription instead", "v1.3")
	def ofOrganizationWithId(organizationId: Int) = DbOrganizationDescription(organizationId)
	
	/**
	 * @param deviceId Device id
	 * @return An access point to that device's individual descriptions
	 */
	@deprecated("Please use DbDeviceDescription instead", "v1.3")
	def ofDeviceWithId(deviceId: Int) = DbDeviceDescription(deviceId)
	
	/**
	 * @param taskId Task id
	 * @return An access point to individual descriptions of that task type
	 */
	@deprecated("Please use DbTaskDescription instead", "v1.3")
	def ofTaskWithId(taskId: Int) = DbTaskDescription(taskId)
	
	/**
	 * @param roleId User role id
	 * @return An access point to individual descriptions of that user role
	 */
	@deprecated("Please use DbUserRoleDescription instead", "v1.3")
	def ofUserRoleWithId(roleId: Int) = DbUserRoleDescription(roleId)
	
	/**
	 * @param languageId Language id
	 * @return An access point to that language's individual descriptions
	 */
	@deprecated("Please use DbLanguageDescription instead", "v1.3")
	def ofLanguageWithId(languageId: Int) = DbLanguageDescription(languageId)
	
	/**
	 * @param roleId Id of the targeted description role
	 * @return An access point to that description role's individual descriptions
	 */
	@deprecated("Please use DbDescriptionRoleDescription instead", "v1.3")
	def ofDescriptionRoleWithId(roleId: Int) = DbDescriptionRoleDescription(roleId)
	/**
	  * @param descriptionRole A description role
	  * @return An access point to that description role's individual descriptions
	  */
	@deprecated("Please use DbDescriptionRoleDescription instead", "v1.3")
	def of(descriptionRole: DescriptionRoleIdWrapper) = ofDescriptionRoleWithId(descriptionRole.id)
}