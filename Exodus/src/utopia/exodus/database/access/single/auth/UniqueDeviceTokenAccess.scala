package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.DeviceTokenFactory
import utopia.exodus.database.model.auth.DeviceTokenModel
import utopia.exodus.model.stored.auth.DeviceToken
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct DeviceTokens.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
trait UniqueDeviceTokenAccess 
	extends SingleRowModelAccess[DeviceToken] 
		with DistinctModelAccess[DeviceToken, Option[DeviceToken], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the device this token provides access to. None if no instance (or value) was found.
	  */
	def deviceId(implicit connection: Connection) = pullColumn(model.deviceIdColumn).int
	/**
	  * Id of the user who owns this token and presumably the linked device, 
		also. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	/**
	  * Textual representation of this token. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	/**
	  * Time when this device use was started / authenticated. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	/**
	  * Time when this token was invalidated, if applicable. None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DeviceTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DeviceTokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Deprecates this token so that it can't be used anymore
	  * @param connection Implicit DB Connection
	  * @return Whether any token was affected
	  */
	def deprecate()(implicit connection: Connection) = deprecatedAfter = Now
	
	/**
	  * Updates the created of the targeted DeviceToken instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the deprecatedAfter of the targeted DeviceToken instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	/**
	  * Updates the deviceId of the targeted DeviceToken instance(s)
	  * @param newDeviceId A new deviceId to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def deviceId_=(newDeviceId: Int)(implicit connection: Connection) = 
		putColumn(model.deviceIdColumn, newDeviceId)
	/**
	  * Updates the token of the targeted DeviceToken instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	/**
	  * Updates the userId of the targeted DeviceToken instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

