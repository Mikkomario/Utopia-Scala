package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.DeviceTokenFactory
import utopia.exodus.model.partial.auth.DeviceTokenData
import utopia.exodus.model.stored.auth.DeviceToken
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter

/**
  * Used for constructing DeviceTokenModel instances and for inserting DeviceTokens to the database
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object DeviceTokenModel 
	extends DataInserter[DeviceTokenModel, DeviceToken, DeviceTokenData] 
		with DeprecatableAfter[DeviceTokenModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains DeviceToken deviceId
	  */
	val deviceIdAttName = "deviceId"
	
	/**
	  * Name of the property that contains DeviceToken userId
	  */
	val userIdAttName = "userId"
	
	/**
	  * Name of the property that contains DeviceToken token
	  */
	val tokenAttName = "token"
	
	/**
	  * Name of the property that contains DeviceToken created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains DeviceToken deprecatedAfter
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains DeviceToken deviceId
	  */
	def deviceIdColumn = table(deviceIdAttName)
	
	/**
	  * Column that contains DeviceToken userId
	  */
	def userIdColumn = table(userIdAttName)
	
	/**
	  * Column that contains DeviceToken token
	  */
	def tokenColumn = table(tokenAttName)
	
	/**
	  * Column that contains DeviceToken created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains DeviceToken deprecatedAfter
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = DeviceTokenFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: DeviceTokenData) = 
		apply(None, Some(data.deviceId), Some(data.userId), Some(data.token), Some(data.created), 
			data.deprecatedAfter)
	
	override def complete(id: Value, data: DeviceTokenData) = DeviceToken(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this device use was started / authenticated
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when this token was invalidated, if applicable
	  * @return A model containing only the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param deviceId Id of the device this token provides access to
	  * @return A model containing only the specified deviceId
	  */
	def withDeviceId(deviceId: Int) = apply(deviceId = Some(deviceId))
	
	/**
	  * @param id A DeviceToken id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param token Textual representation of this token
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
	
	/**
	  * @param userId Id of the user who owns this token and presumably the linked device, also
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with DeviceTokens in the database
  * @param id DeviceToken database id
  * @param deviceId Id of the device this token provides access to
  * @param userId Id of the user who owns this token and presumably the linked device, also
  * @param token Textual representation of this token
  * @param created Time when this device use was started / authenticated
  * @param deprecatedAfter Time when this token was invalidated, if applicable
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class DeviceTokenModel(id: Option[Int] = None, deviceId: Option[Int] = None, userId: Option[Int] = None, 
	token: Option[String] = None, created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None) 
	extends StorableWithFactory[DeviceToken]
{
	// IMPLEMENTED	--------------------
	
	override def factory = DeviceTokenModel.factory
	
	override def valueProperties = 
	{
		import DeviceTokenModel._
		Vector("id" -> id, deviceIdAttName -> deviceId, userIdAttName -> userId, tokenAttName -> token, 
			createdAttName -> created, deprecatedAfterAttName -> deprecatedAfter)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter A new deprecatedAfter
	  * @return A new copy of this model with the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param deviceId A new deviceId
	  * @return A new copy of this model with the specified deviceId
	  */
	def withDeviceId(deviceId: Int) = copy(deviceId = Some(deviceId))
	
	/**
	  * @param token A new token
	  * @return A new copy of this model with the specified token
	  */
	def withToken(token: String) = copy(token = Some(token))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}

