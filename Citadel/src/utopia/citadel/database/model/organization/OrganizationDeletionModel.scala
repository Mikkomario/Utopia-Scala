package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.OrganizationDeletionFactory
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.partial.organization.OrganizationDeletionData
import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing OrganizationDeletionModel instances and for inserting OrganizationDeletions to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object OrganizationDeletionModel 
	extends DataInserter[OrganizationDeletionModel, OrganizationDeletion, OrganizationDeletionData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains OrganizationDeletion organizationId
	  */
	val organizationIdAttName = "organizationId"
	
	/**
	  * Name of the property that contains OrganizationDeletion actualization
	  */
	val actualizationAttName = "actualization"
	
	/**
	  * Name of the property that contains OrganizationDeletion creatorId
	  */
	val creatorIdAttName = "creatorId"
	
	/**
	  * Name of the property that contains OrganizationDeletion created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains OrganizationDeletion organizationId
	  */
	def organizationIdColumn = table(organizationIdAttName)
	
	/**
	  * Column that contains OrganizationDeletion actualization
	  */
	def actualizationColumn = table(actualizationAttName)
	
	/**
	  * Column that contains OrganizationDeletion creatorId
	  */
	def creatorIdColumn = table(creatorIdAttName)
	
	/**
	  * Column that contains OrganizationDeletion created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = OrganizationDeletionFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: OrganizationDeletionData) = 
		apply(None, Some(data.organizationId), Some(data.actualization), Some(data.creatorId), 
			Some(data.created))
	
	override def complete(id: Value, data: OrganizationDeletionData) = OrganizationDeletion(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param actualization Time when this deletion is/was scheduled to actualize
	  * @return A model containing only the specified actualization
	  */
	def withActualization(actualization: Instant) = apply(actualization = Some(actualization))
	
	/**
	  * @param created Time when this deletion was requested
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param creatorId Id of the user who requested organization deletion
	  * @return A model containing only the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = apply(creatorId = Some(creatorId))
	
	/**
	  * @param id A OrganizationDeletion id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param organizationId Id of the organization whose deletion was requested
	  * @return A model containing only the specified organizationId
	  */
	def withOrganizationId(organizationId: Int) = apply(organizationId = Some(organizationId))
}

/**
  * Used for interacting with OrganizationDeletions in the database
  * @param id OrganizationDeletion database id
  * @param organizationId Id of the organization whose deletion was requested
  * @param actualization Time when this deletion is/was scheduled to actualize
  * @param creatorId Id of the user who requested organization deletion
  * @param created Time when this deletion was requested
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationDeletionModel(id: Option[Int] = None, organizationId: Option[Int] = None, 
	actualization: Option[Instant] = None, creatorId: Option[Int] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[OrganizationDeletion]
{
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionModel.factory
	
	override def valueProperties = 
	{
		import OrganizationDeletionModel._
		Vector("id" -> id, organizationIdAttName -> organizationId, actualizationAttName -> actualization, 
			creatorIdAttName -> creatorId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param actualization A new actualization
	  * @return A new copy of this model with the specified actualization
	  */
	def withActualization(actualization: Instant) = copy(actualization = Some(actualization))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param creatorId A new creatorId
	  * @return A new copy of this model with the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = copy(creatorId = Some(creatorId))
	
	/**
	  * @param organizationId A new organizationId
	  * @return A new copy of this model with the specified organizationId
	  */
	def withOrganizationId(organizationId: Int) = copy(organizationId = Some(organizationId))
}

