package utopia.citadel.database.model.user

import java.time.Instant
import utopia.citadel.database.Tables
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable
import utopia.vault.nosql.factory.Deprecatable
import utopia.vault.sql.{Condition, Exists}

// TODO: Once there exists a factory object for this class, move deprecatable there
object UserDeviceModel extends Deprecatable
{
	// ATTRIBUTES	----------------------
	
	/**
	  * Name of the attribute containing the device id
	  */
	val deviceIdAttName = "deviceId"
	
	/**
	  * Name of the attribute that contians the user id
	  */
	val userIdAttName = "userId"
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Table used by this model
	  */
	def table = Tables.userDevice
	
	/**
	  * @return A model that has just been marked as deprecated
	  */
	def nowDeprecated = withDeprecatedAfter(Now)
	
	
	// IMPLEMENTED	----------------------
	
	override def nonDeprecatedCondition = table("deprecatedAfter").isNull
	
	
	// OTHER	--------------------------
	
	/**
	  * @param userId Id of described user
	  * @return A model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * @param deviceId Id of the linked device
	  * @return A model with only device id set
	  */
	def withDeviceId(deviceId: Int) = apply(deviceId = Some(deviceId))
	
	/**
	  * @param created Row creation time
	  * @return A model with only creation time set
	  */
	def withCreationTime(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecationTime Link deprecation time
	  * @return A model with only deprecation time set
	  */
	def withDeprecatedAfter(deprecationTime: Instant) = apply(deprecatedAfter = Some(deprecationTime))
	
	/**
	  * Inserts a new connection between a user and a client device
	  * @param userId Id of the user
	  * @param deviceId Id of the device
	  * @param connection DB Connection (implicit)
	  * @return Id of the newly created link
	  */
	def insert(userId: Int, deviceId: Int)(implicit connection: Connection) =
		apply(None, Some(userId), Some(deviceId)).insert().getInt
	
	/**
	  * Checks whether there exists a user device connection with specified condition
	  * @param condition A condition
	  * @param connection DB Connection (implicit)
	  * @return Whether any row fulfills the specified condition
	  */
	def exists(condition: Condition)(implicit connection: Connection) = Exists(table, condition)
}

/**
  * Registers links between users and devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
case class UserDeviceModel(id: Option[Int] = None, userId: Option[Int] = None, deviceId: Option[Int] = None,
						   created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None) extends Storable
{
	// IMPLEMENTED	---------------------------------
	
	override def table = UserDeviceModel.table
	
	override def valueProperties = Vector("id" -> id, "userId" -> userId, "deviceId" -> deviceId, "created" -> created,
		"deprecatedAfter" -> deprecatedAfter)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * @param deviceId Id of linked device
	  * @return A copy of this model with specified device id
	  */
	def withDeviceId(deviceId: Int) = copy(deviceId = Some(deviceId))
}
