package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.DeviceTokenFactory
import utopia.exodus.database.model.auth.DeviceTokenModel
import utopia.exodus.model.partial.auth.DeviceTokenData
import utopia.exodus.model.stored.auth.DeviceToken
import utopia.exodus.util.UuidGenerator
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual DeviceTokens
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbDeviceToken 
	extends SingleRowModelAccess[DeviceToken] with NonDeprecatedView[DeviceToken] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DeviceTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DeviceTokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted DeviceToken instance
	  * @return An access point to that DeviceToken
	  */
	def apply(id: Int) = DbSingleDeviceToken(id)
	
	/**
	  * @param token A device token
	  * @return An access point to that token
	  */
	def apply(token: String) = new DbDeviceTokenWrapperAccess(token)
	
	/**
	  * @param deviceId A device id
	  * @return An access point to device token linked with that device
	  */
	def forDeviceWithId(deviceId: Int) = new DbTokenForDevice(deviceId)
	
	
	// NESTED   -------------------
	
	class DbTokenForDevice(deviceId: Int) extends UniqueDeviceTokenAccess with SubView
	{
		// ATTRIBUTES   ----------
		
		override lazy val filterCondition = model.withDeviceId(deviceId).toCondition
		
		
		// IMPLEMENTED  ----------
		
		override protected def parent = DbDeviceToken
		override protected def defaultOrdering = None
		
		
		// OTHER    --------------
		
		/**
		  * @param userId A user id
		  * @param connection Implicit DB Connection
		  * @return Whether this device token is currently owned by that user
		  */
		def isHeldByUserWithId(userId: Int)(implicit connection: Connection) =
			exists(model.withUserId(userId).toCondition)
		
		/**
		  * Assigns this device (and token) to a new user - Removes tokens from other device users
		  * @param userId Id of the user to whom this device/token will be assigned
		  * @param newToken New token to assign (default = generate automatically)
		  * @param connection Implicit DB Connection
		  * @return Newly inserted token
		  */
		def assignToUserWithId(userId: Int, newToken: String)(implicit connection: Connection) =
		{
			// Deprecates the existing key and replaces it with a new one
			deprecate()
			model.insert(DeviceTokenData(deviceId, userId, newToken))
		}
		/**
		  * Assigns this device (and token) to a new user - Removes tokens from other device users
		  * @param userId Id of the user to whom this device/token will be assigned
		  * @param connection Implicit DB Connection
		  * @return Newly inserted token
		  */
		def assignToUserWithId(userId: Int)
		                      (implicit connection: Connection, uuidGenerator: UuidGenerator): DeviceToken =
			assignToUserWithId(userId, uuidGenerator.next())
		
		/**
		  * Deprecates this token if it is being held by the specified user
		  * @param userId Id of the user who should no longer have this device/token
		  * @param connection Implicit DB Connection
		  * @return Whether a token was deprecated (false if the user didn't have a token on this device)
		  */
		def releaseFromUserWithId(userId: Int)(implicit connection: Connection) =
			model.nowDeprecated.updateWhere(mergeCondition(model.withUserId(userId))) > 0
	}
	
	class DbDeviceTokenWrapperAccess(token: String) extends UniqueDeviceTokenAccess with SubView
	{
		override protected def parent = DbDeviceToken
		override protected def defaultOrdering = None
		
		override def filterCondition = model.withToken(token).toCondition
	}
}

