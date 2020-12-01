package utopia.exodus.database.access.single

import utopia.exodus.database.factory.description.DescriptionLinkFactory
import utopia.exodus.database.model.description.{DescriptionLinkModel, DescriptionLinkModelFactory}
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.model.immutable.Storable

/**
  * Used for accessing various types of individual descriptions
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  */
object DbDescription
{
	// OTHER	----------------------------
	
	/**
	  * @param organizationId Organization id
	  * @return An access point to that organization's individual descriptions
	  */
	def ofOrganizationWithId(organizationId: Int) =
		DescriptionOfSingle(organizationId, DescriptionLinkFactory.organization, DescriptionLinkModel.organization)
	
	/**
	  * @param deviceId Device id
	  * @return An access point to that device's individual descriptions
	  */
	def ofDeviceWithId(deviceId: Int) =
		DescriptionOfSingle(deviceId, DescriptionLinkFactory.device, DescriptionLinkModel.device)
	
	/**
	  * @param taskId Task id
	  * @return An access point to individual descriptions of that task type
	  */
	def ofTaskWithId(taskId: Int) = DescriptionOfSingle(taskId, DescriptionLinkFactory.task,
		DescriptionLinkModel.task)
	
	/**
	  * @param roleId User role id
	  * @return An access point to individual descriptions of that user role
	  */
	def ofRoleWithId(roleId: Int) =
		DescriptionOfSingle(roleId, DescriptionLinkFactory.userRole, DescriptionLinkModel.userRole)
	
	/**
	  * @param languageId Language id
	  * @return An access point to that language's individual descriptions
	  */
	def ofLanguageWithId(languageId: Int) =
		DescriptionOfSingle(languageId, DescriptionLinkFactory.language, DescriptionLinkModel.language)
	
	
	// NESTED	----------------------------
	
	case class DescriptionOfSingle(targetId: Int, factory: DescriptionLinkFactory[DescriptionLink],
									linkModelFactory: DescriptionLinkModelFactory[Storable])
		extends DescriptionLinkAccess
	{
		// IMPLEMENTED	--------------------
		
		override val globalCondition = Some(linkModelFactory.withTargetId(targetId).toCondition &&
			factory.nonDeprecatedCondition)
	}
}
