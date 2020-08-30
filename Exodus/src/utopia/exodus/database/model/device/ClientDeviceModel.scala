package utopia.exodus.database.model.device

import utopia.exodus.database.Tables
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable

object ClientDeviceModel
{
	// COMPUTED	-----------------------------
	
	def table = Tables.clientDevice
	
	
	// OTHER	-----------------------------
	
	/**
	  * Inserts a new client device to the DB
	  * @param creatorId Id of the user creating this device registration
	  * @param connection DB Connection
	  * @return New client device id
	  */
	def insert(creatorId: Int)(implicit connection: Connection) = apply(None, Some(creatorId)).insert().getInt
}

/**
  * Used for interacting with user devices in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class ClientDeviceModel(id: Option[Int] = None, creatorId: Option[Int] = None) extends Storable
{
	override def table = ClientDeviceModel.table
	
	override def valueProperties = Vector("id" -> id, "creatorId" -> creatorId)
}
