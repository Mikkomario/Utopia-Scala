package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.OrganizationDeletionCancellationFactory
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.OrganizationDeletionCancellationData
import utopia.metropolis.model.stored.organization.OrganizationDeletionCancellation
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing OrganizationDeletionCancellationModel instances and for inserting OrganizationDeletionCancellations to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object OrganizationDeletionCancellationModel 
	extends DataInserter[OrganizationDeletionCancellationModel, OrganizationDeletionCancellation, 
		OrganizationDeletionCancellationData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains OrganizationDeletionCancellation deletionId
	  */
	val deletionIdAttName = "deletionId"
	/**
	  * Name of the property that contains OrganizationDeletionCancellation creatorId
	  */
	val creatorIdAttName = "creatorId"
	/**
	  * Name of the property that contains OrganizationDeletionCancellation created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains OrganizationDeletionCancellation deletionId
	  */
	def deletionIdColumn = table(deletionIdAttName)
	/**
	  * Column that contains OrganizationDeletionCancellation creatorId
	  */
	def creatorIdColumn = table(creatorIdAttName)
	/**
	  * Column that contains OrganizationDeletionCancellation created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = OrganizationDeletionCancellationFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: OrganizationDeletionCancellationData) = 
		apply(None, Some(data.deletionId), data.creatorId, Some(data.created))
	
	override def complete(id: Value, data: OrganizationDeletionCancellationData) = 
		OrganizationDeletionCancellation(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this OrganizationDeletionCancellation was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param creatorId Id of the user who cancelled the referenced organization deletion, if still known
	  * @return A model containing only the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = apply(creatorId = Some(creatorId))
	/**
	  * @param deletionId Id of the cancelled deletion
	  * @return A model containing only the specified deletionId
	  */
	def withDeletionId(deletionId: Int) = apply(deletionId = Some(deletionId))
	/**
	  * @param id A OrganizationDeletionCancellation id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with OrganizationDeletionCancellations in the database
  * @param id OrganizationDeletionCancellation database id
  * @param deletionId Id of the cancelled deletion
  * @param creatorId Id of the user who cancelled the referenced organization deletion, if still known
  * @param created Time when this OrganizationDeletionCancellation was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationDeletionCancellationModel(id: Option[Int] = None, deletionId: Option[Int] = None, 
	creatorId: Option[Int] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[OrganizationDeletionCancellation]
{
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionCancellationModel.factory
	
	override def valueProperties = 
	{
		import OrganizationDeletionCancellationModel._
		Vector("id" -> id, deletionIdAttName -> deletionId, creatorIdAttName -> creatorId, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
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
	  * @param deletionId A new deletionId
	  * @return A new copy of this model with the specified deletionId
	  */
	def withDeletionId(deletionId: Int) = copy(deletionId = Some(deletionId))
}

