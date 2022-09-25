package utopia.metropolis.model.partial.device

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

@deprecated("This class will be removed in a future release", "v2.1")
object ClientDeviceUserData extends FromModelFactoryWithSchema[ClientDeviceUserData]
{
	override val schema = ModelDeclaration("device_id" -> IntType, "user_id" -> IntType)
	
	override protected def fromValidatedModel(model: Model) =
		apply(model("device_id"), model("user_id"), model("created"), model("deprecated_after"))
}

/**
  * Links users to the devices they are using
  * @param deviceId Id of the device the referenced user is/was using
  * @param userId Id of the user who is/was using this device
  * @param created Time when this link was registered (device use started)
  * @param deprecatedAfter Time when device use ended
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
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

