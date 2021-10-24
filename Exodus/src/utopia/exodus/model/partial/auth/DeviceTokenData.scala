package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Used as a refresh token to generate device-specific session tokens on private devices
  * @param deviceId Id of the device this token provides access to
  * @param userId Id of the user who owns this token and presumably the linked device, also
  * @param token Textual representation of this token
  * @param created Time when this device use was started / authenticated
  * @param deprecatedAfter Time when this token was invalidated, if applicable
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class DeviceTokenData(deviceId: Int, userId: Int, token: String, created: Instant = Now, 
	deprecatedAfter: Option[Instant] = None) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this DeviceToken has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	
	/**
	  * Whether this DeviceToken is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("device_id" -> deviceId, "user_id" -> userId, "token" -> token, "created" -> created, 
			"deprecated_after" -> deprecatedAfter))
}

