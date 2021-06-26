package utopia.citadel.database.model.organization

import java.time.Instant

import utopia.citadel.database.Tables
import utopia.citadel.database.factory.organization.DeletionFactory
import utopia.vault.model.immutable.Storable
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.DeletionData
import utopia.metropolis.model.stored.organization.Deletion
import utopia.vault.database.Connection

object DeletionModel
{
	// ATTRIBUTES	----------------------------
	
	/**
	  * Name of the attribute that contains targeted organization's id
	  */
	val organizationIdAttName = "organizationId"
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return Table used by this model
	  */
	def table = Tables.organizationDeletion
	
	/**
	  * @return Column that contains targeted organization's id
	  */
	def organizationIdColumn = table(organizationIdAttName)
	
	
	// OTHER	--------------------------------
	
	/**
	  * @param organizationId Id of targeted organization
	  * @return A model with only organization id set
	  */
	def withOrganizationId(organizationId: Int) = apply(organizationId = Some(organizationId))
	
	/**
	  * @param actualization Actualization time
	  * @return A model with only actualization time set
	  */
	def withActualizationTime(actualization: Instant) = apply(actualizationTime = Some(actualization))
	
	/**
	  * Inserts a new organization deletion attempt to DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted deletion
	  */
	def insert(data: DeletionData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.organizationId), Some(data.creatorId),
			Some(data.actualizationTime)).insert().getInt
		Deletion(newId, data)
	}
}

/**
  * Used for interacting with organization deletions in DB
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.0
  */
case class DeletionModel(id: Option[Int] = None, organizationId: Option[Int] = None, creatorId: Option[Int] = None,
						 actualizationTime: Option[Instant] = None) extends Storable
{
	import DeletionModel._
	
	override def table = DeletionFactory.table
	
	override def valueProperties = Vector("id" -> id, organizationIdAttName -> organizationId, "creatorId" -> creatorId,
		"actualization" -> actualizationTime)
}
