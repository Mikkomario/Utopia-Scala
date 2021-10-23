package utopia.metropolis.model.partial.device

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Links users to the devices they are using
  * @param deviceId Id of the device the referenced user is/was using
  * @param userId Id of the user who is/was using this device
  * @param created Time when this link was registered (device use started)
  * @param deprecatedAfter Time when device use ended
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class ClientDeviceUserData(deviceId: Int, userId: Int, created: Instant = Now, 
	deprecatedAfter: Option[Instant] = None) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this ClientDeviceUser has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	
	/**
	  * Whether this ClientDeviceUser is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("device_id" -> deviceId, "user_id" -> userId, "created" -> created, 
			"deprecated_after" -> deprecatedAfter))
}

