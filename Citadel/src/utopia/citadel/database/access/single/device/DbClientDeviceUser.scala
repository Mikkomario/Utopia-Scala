package utopia.citadel.database.access.single.device

import utopia.citadel.database.factory.device.ClientDeviceUserFactory
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.metropolis.model.partial.device.ClientDeviceUserData
import utopia.metropolis.model.stored.device.ClientDeviceUser
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual ClientDeviceUsers
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
object DbClientDeviceUser 
	extends SingleRowModelAccess[ClientDeviceUser] with NonDeprecatedView[ClientDeviceUser] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ClientDeviceUserModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ClientDeviceUserFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted ClientDeviceUser instance
	  * @return An access point to that ClientDeviceUser
	  */
	def apply(id: Int) = DbSingleClientDeviceUser(id)
	
	/**
	  * @param deviceId Id of the targeted device
	  * @param userId Id of the targeted user
	  * @return An access point to a possible link between that device and user
	  */
	def linkBetween(deviceId: Int, userId: Int) = new DbUniqueClientDeviceUserLink(deviceId, userId)
	
	
	// NESTED   -------------------
	
	class DbUniqueClientDeviceUserLink(deviceId: Int, userId: Int) extends UniqueClientDeviceUserAccess with SubView
	{
		// IMPLEMENTED  -----------
		
		override protected def parent = DbClientDeviceUser
		
		override def filterCondition = model.withDeviceId(deviceId).withUserId(userId).toCondition
		
		
		// OTHER    ---------------
		
		/**
		  * Reads this link or creates one
		  * @param connection Implicit DB Connection
		  * @return Read or inserted link
		  */
		def getOrInsert()(implicit connection: Connection) =
			pull.getOrElse { model.insert(ClientDeviceUserData(deviceId, userId)) }
		/**
		  * Creates a new link between this device and user, if there doesn't exist one already
		  * @param connection Implicit DB Connection
		  * @return Created link. None if there already existed a link.
		  */
		def createIfEmpty()(implicit connection: Connection) =
		{
			if (isEmpty)
				None
			else
				Some(model.insert(ClientDeviceUserData(deviceId, userId)))
		}
	}
}

