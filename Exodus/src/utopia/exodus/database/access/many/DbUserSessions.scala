package utopia.exodus.database.access.many

import utopia.exodus.database.factory.user.SessionFactory
import utopia.exodus.database.model.user.SessionModel
import utopia.exodus.model.stored.UserSession
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess

/**
  * Used for accessing multiple user sessions at once
  * @author Mikko Hilpinen
  * @since 3.5.2020, v2
  */
object DbUserSessions extends ManyModelAccess[UserSession]
{
	// IMPLEMENTED	----------------------------
	
	override def factory = SessionFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	--------------------------------
	
	private def model = SessionModel
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param deviceId Id of targeted device
	  * @return An access point to active sessions on that device
	  */
	def forDeviceWithId(deviceId: Int) = new DeviceSessions(deviceId)
	
	
	// NESTED	---------------------------------
	
	class DeviceSessions(deviceId: Int) extends ManyModelAccess[UserSession]
	{
		// COMPUTED	-----------------------------
		
		private def condition = DbUserSessions.mergeCondition(model.withDeviceId(deviceId).toCondition)
		
		
		// IMPLEMENTED	-------------------------
		
		override def factory = DbUserSessions.factory
		
		override def globalCondition = Some(condition)
		
		
		// OTHER	-----------------------------
		
		/**
		  * Terminates all user sessions accessible from this accessor
		  * @param connection DB Connection (implicit)
		  * @return Number of sessions that were terminated
		  */
		def end()(implicit connection: Connection) = model.nowLoggedOut.updateWhere(condition)
	}
}
