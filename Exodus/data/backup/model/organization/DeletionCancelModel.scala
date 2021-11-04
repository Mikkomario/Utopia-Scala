package utopia.exodus.database.model.organization

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.organization.DeletionCancelFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.DeletionCancelData
import utopia.metropolis.model.stored.organization.DeletionCancel
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

@deprecated("Please use the Citadel version instead", "v2.0")
object DeletionCancelModel
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Table used by this model
	  */
	def table = Tables.organizationDeletionCancellation
	
	
	// OTHER	-----------------------------
	
	/**
	  * Inserts a new deletion cancel to the DB
	  * @param data Data to insert
	  * @param connection DB Connection
	  * @return Newly inserted cancellation
	  */
	def insert(data: DeletionCancelData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.deletionId), data.creatorId).insert().getInt
		DeletionCancel(newId, data)
	}
}

/**
  * Used for interacting with organization deletion cancellation data
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
case class DeletionCancelModel(id: Option[Int] = None, deletionId: Option[Int] = None, creatorId: Option[Int] = None)
	extends StorableWithFactory[DeletionCancel]
{
	override def factory = DeletionCancelFactory
	
	override def valueProperties = Vector("id" -> id, "deletionId" -> deletionId, "creatorId" -> creatorId)
}
