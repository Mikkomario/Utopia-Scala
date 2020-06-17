package utopia.exodus.database.access.single

import utopia.exodus.database.factory.device.DeviceKeyFactory
import utopia.exodus.database.model.device.DeviceKeyModel
import utopia.exodus.model.stored.DeviceKey
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleIdModelAccess, SingleModelAccess}

/**
  * Used for accessing individual device keys in DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v2
  */
object DbDeviceKey extends SingleModelAccess[DeviceKey]
{
	// IMPLEMENTED	------------------------------------
	
	override def factory = DeviceKeyFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	----------------------------------------
	
	private def model = DeviceKeyModel
	
	
	// OTHER	----------------------------------------
	
	/**
	  * @param id Device key id
	  * @return An access point to a device key by that id
	  */
	def apply(id: Int) = new SingleDeviceKeyById(id)
	
	/**
	  * @param key Authorization key
	  * @param connection DB Connection (implicit)
	  * @return A device key that matches specified authorization key
	  */
	def matching(key: String)(implicit connection: Connection) = find(model.withKey(key).toCondition)
	
	
	// NESTED	-----------------------------------------
	
	class SingleDeviceKeyById(deviceKeyId: Int) extends SingleIdModelAccess[DeviceKey](deviceKeyId, DbDeviceKey.factory)
	{
		/**
		  * Invalidates this key so that it can no longer be used for authenticating requests
		  * @param connection DB Connection (implicit)
		  * @return Whether any change was made
		  */
		def invalidate()(implicit connection: Connection) = DbDeviceKey.model.nowDeprecated.updateWhere(
			condition && DbDeviceKey.factory.nonDeprecatedCondition) > 0
	}
}
