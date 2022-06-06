package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.DeviceTokenFactory
import utopia.exodus.database.model.auth.DeviceTokenModel
import utopia.exodus.model.stored.auth.DeviceToken
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyDeviceTokensAccess
{
	// NESTED	--------------------
	
	private class ManyDeviceTokensSubView(override val parent: ManyRowModelAccess[DeviceToken], 
		override val filterCondition: Condition) 
		extends ManyDeviceTokensAccess with SubView
}

/**
  * A common trait for access points which target multiple DeviceTokens at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
trait ManyDeviceTokensAccess
	extends ManyRowModelAccess[DeviceToken] with Indexed with FilterableView[ManyDeviceTokensAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * deviceIds of the accessible DeviceTokens
	  */
	def deviceIds(implicit connection: Connection) = 
		pullColumn(model.deviceIdColumn).flatMap { value => value.int }
	/**
	  * userIds of the accessible DeviceTokens
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	/**
	  * tokens of the accessible DeviceTokens
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	/**
	  * creationTimes of the accessible DeviceTokens
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	/**
	  * deprecationTimes of the accessible DeviceTokens
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DeviceTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DeviceTokenFactory
	
	override def filter(additionalCondition: Condition): ManyDeviceTokensAccess = 
		new ManyDeviceTokensAccess.ManyDeviceTokensSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted DeviceToken instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the deprecatedAfter of the targeted DeviceToken instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	/**
	  * Updates the deviceId of the targeted DeviceToken instance(s)
	  * @param newDeviceId A new deviceId to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def deviceIds_=(newDeviceId: Int)(implicit connection: Connection) = 
		putColumn(model.deviceIdColumn, newDeviceId)
	/**
	  * Updates the token of the targeted DeviceToken instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	/**
	  * Updates the userId of the targeted DeviceToken instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any DeviceToken instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

