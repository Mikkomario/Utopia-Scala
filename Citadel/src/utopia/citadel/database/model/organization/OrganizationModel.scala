package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.OrganizationFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.OrganizationData
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing OrganizationModel instances and for inserting Organizations to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object OrganizationModel extends DataInserter[OrganizationModel, Organization, OrganizationData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Organization creatorId
	  */
	val creatorIdAttName = "creatorId"
	
	/**
	  * Name of the property that contains Organization created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Organization creatorId
	  */
	def creatorIdColumn = table(creatorIdAttName)
	
	/**
	  * Column that contains Organization created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = OrganizationFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: OrganizationData) = apply(None, data.creatorId, Some(data.created))
	
	override def complete(id: Value, data: OrganizationData) = Organization(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this Organization was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param creatorId Id of the user who created this organization (if still known)
	  * @return A model containing only the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = apply(creatorId = Some(creatorId))
	
	/**
	  * @param id A Organization id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with Organizations in the database
  * @param id Organization database id
  * @param creatorId Id of the user who created this organization (if still known)
  * @param created Time when this Organization was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class OrganizationModel(id: Option[Int] = None, creatorId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[Organization]
{
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationModel.factory
	
	override def valueProperties = 
	{
		import OrganizationModel._
		Vector("id" -> id, creatorIdAttName -> creatorId, createdAttName -> created)
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
}

