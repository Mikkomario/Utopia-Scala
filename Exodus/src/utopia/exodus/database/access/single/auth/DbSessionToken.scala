package utopia.exodus.database.access.single.auth

import utopia.exodus.database.access.many.auth.DbSessionTokens
import utopia.exodus.database.factory.auth.SessionTokenFactory
import utopia.exodus.database.model.auth.SessionTokenModel
import utopia.exodus.model.partial.auth.SessionTokenData
import utopia.exodus.model.stored.auth.SessionToken
import utopia.exodus.util.UuidGenerator
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

import scala.concurrent.duration.FiniteDuration

/**
  * Used for accessing individual SessionTokens
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbSessionToken 
	extends SingleRowModelAccess[SessionToken] with NonDeprecatedView[SessionToken] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SessionTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SessionTokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted SessionToken instance
	  * @return An access point to that SessionToken
	  */
	def apply(id: Int) = DbSingleSessionToken(id)
	
	/**
	  * @param token A session token
	  * @return An access point to that token's session
	  */
	def apply(token: String) = new DbSessionTokenWrapperAccess(token)
	
	/**
	  * @param userId Id of targeted user
	  * @param deviceId Id of targeted device (None if targeting a deviceless session)
	  * @return An access point to that user's active session on that device (or their deviceless session)
	  */
	def forSession(userId: Int, deviceId: Option[Int]) = new DbUniqueDeviceSession(userId, deviceId)
	/**
	  * @param userId Id of targeted user
	  * @param deviceId Id of targeted device
	  * @return An access point to that user's active session on that device
	  */
	def forDeviceSession(userId: Int, deviceId: Int) = new DbUniqueDeviceSession(userId, Some(deviceId))
	/**
	  * @param userId Id of targeted user
	  * @return An access point to that user's active deviceless session
	  */
	def forDevicelessSession(userId: Int) = new DbUniqueDeviceSession(userId, None)
	
	
	// NESTED   --------------------
	
	class DbSessionTokenWrapperAccess(token: String) extends UniqueSessionTokenAccess with SubView
	{
		override protected def parent = DbSessionToken
		
		override def filterCondition = model.withToken(token).toCondition
	}
	
	class DbUniqueDeviceSession(userId: Int, deviceId: Option[Int]) extends UniqueSessionTokenAccess with SubView
	{
		// ATTRIBUTES   ------------
		
		override lazy val filterCondition =
		{
			val userIdModel = model.withUserId(userId)
			deviceId match
			{
				case Some(deviceId) => userIdModel.withDeviceId(deviceId).toCondition
				case None => userIdModel.toCondition && model.deviceIdColumn.isNull
			}
		}
		
		
		// IMPLEMENTED  ------------
		
		override protected def parent = DbSessionToken
		
		
		// OTHER    ----------------
		
		/**
		  * Starts a new session on this device. Logs out any previous user(s) from this device as well.
		  * @param preferredModelStyle Model style preferred to be used during this session (optional)
		  * @param maxDuration Maximum time duration before this session closes automatically (default = 24 hours)
		  * @param connection DB Connection (implicit)
		  * @param uuidGenerator A token generator to use (implicit)
		  * @return New session token
		  */
		def start(preferredModelStyle: Option[ModelStyle] = None, maxDuration: FiniteDuration = 24.hours)
		         (implicit connection: Connection, uuidGenerator: UuidGenerator) =
		{
			// Before starting a new session, makes sure to terminate existing user sessions for this device
			// On deviceless sessions, terminates the previous deviceless session
			deviceId match
			{
				case Some(deviceId) => DbSessionTokens.onDeviceWithId(deviceId).logOut()
				case None => logOut()
			}
			// Creates a new session that lasts for 24 hours or until logged out
			model.insert(SessionTokenData(userId, uuidGenerator.next(), Now + maxDuration, deviceId,
				preferredModelStyle))
		}
	}
}

