package utopia.citadel.database.access.many.device

import java.time.Instant
import utopia.citadel.database.factory.device.ClientDeviceUserFactory
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.device.ClientDeviceUser
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyClientDeviceUsersAccess
{
	// NESTED	--------------------
	
	private class ManyClientDeviceUsersSubView(override val parent: ManyRowModelAccess[ClientDeviceUser], 
		override val filterCondition: Condition) 
		extends ManyClientDeviceUsersAccess with SubView
}

/**
  * A common trait for access points which target multiple ClientDeviceUsers at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
trait ManyClientDeviceUsersAccess
	extends ManyRowModelAccess[ClientDeviceUser] with Indexed with FilterableView[ManyClientDeviceUsersAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * deviceIds of the accessible ClientDeviceUsers
	  */
	def deviceIds(implicit connection: Connection) = 
		pullColumn(model.deviceIdColumn).flatMap { value => value.int }
	/**
	  * userIds of the accessible ClientDeviceUsers
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	/**
	  * creationTimes of the accessible ClientDeviceUsers
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	/**
	  * deprecationTimes of the accessible ClientDeviceUsers
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ClientDeviceUserModel
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = ClientDeviceUserFactory
	
	override def filter(additionalCondition: Condition): ManyClientDeviceUsersAccess =
		new ManyClientDeviceUsersAccess.ManyClientDeviceUsersSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param deviceId A device id
	  * @return An access point to device-user-links on that device
	  */
	def onDeviceWithId(deviceId: Int) = filter(model.withDeviceId(deviceId).toCondition)
	/**
	  * @param userId Id of the linked user
	  * @return An access point to device links concerning that user only
	  */
	def withUserId(userId: Int) = filter(model.withUserId(userId).toCondition)
	
	/**
	  * Updates the created of the targeted ClientDeviceUser instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the deprecatedAfter of the targeted ClientDeviceUser instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	/**
	  * Updates the deviceId of the targeted ClientDeviceUser instance(s)
	  * @param newDeviceId A new deviceId to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def deviceIds_=(newDeviceId: Int)(implicit connection: Connection) = 
		putColumn(model.deviceIdColumn, newDeviceId)
	/**
	  * Updates the userId of the targeted ClientDeviceUser instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

