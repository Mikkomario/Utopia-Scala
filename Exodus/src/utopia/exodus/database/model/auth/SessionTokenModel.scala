package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.SessionTokenFactory
import utopia.exodus.model.partial.auth.SessionTokenData
import utopia.exodus.model.stored.auth.SessionToken
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for constructing SessionTokenModel instances and for inserting SessionTokens to the database
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object SessionTokenModel 
	extends DataInserter[SessionTokenModel, SessionToken, SessionTokenData] with Deprecatable
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains SessionToken userId
	  */
	val userIdAttName = "userId"
	/**
	  * Name of the property that contains SessionToken token
	  */
	val tokenAttName = "token"
	/**
	  * Name of the property that contains SessionToken expires
	  */
	val expiresAttName = "expires"
	/**
	  * Name of the property that contains SessionToken deviceId
	  */
	val deviceIdAttName = "deviceId"
	/**
	  * Name of the property that contains SessionToken modelStylePreference
	  */
	val modelStylePreferenceAttName = "modelStyleId"
	/**
	  * Name of the property that contains SessionToken created
	  */
	val createdAttName = "created"
	/**
	  * Name of the property that contains SessionToken loggedOut
	  */
	val loggedOutAttName = "loggedOut"
	
	/**
	  * A condition that only accepts tokens that haven't been terminated by logging out
	  */
	lazy val notLoggedOutCondition = loggedOutColumn.isNull
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains SessionToken userId
	  */
	def userIdColumn = table(userIdAttName)
	/**
	  * Column that contains SessionToken token
	  */
	def tokenColumn = table(tokenAttName)
	/**
	  * Column that contains SessionToken expires
	  */
	def expiresColumn = table(expiresAttName)
	/**
	  * Column that contains SessionToken deviceId
	  */
	def deviceIdColumn = table(deviceIdAttName)
	/**
	  * Column that contains SessionToken modelStylePreference
	  */
	def modelStylePreferenceColumn = table(modelStylePreferenceAttName)
	/**
	  * Column that contains SessionToken created
	  */
	def createdColumn = table(createdAttName)
	/**
	  * Column that contains SessionToken loggedOut
	  */
	def loggedOutColumn = table(loggedOutAttName)
	/**
	  * The factory object used by this model type
	  */
	def factory = SessionTokenFactory
	
	override def nonDeprecatedCondition = notLoggedOutCondition && expiresColumn > Now
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: SessionTokenData) = 
		apply(None, Some(data.userId), Some(data.token), Some(data.expires), data.deviceId, 
			data.modelStylePreference, Some(data.created), data.loggedOut)
	
	override def complete(id: Value, data: SessionTokenData) = SessionToken(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this session was started
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deviceId Id of the device on which this session is, if applicable
	  * @return A model containing only the specified deviceId
	  */
	def withDeviceId(deviceId: Int) = apply(deviceId = Some(deviceId))
	
	/**
	  * @param expires Time when this token expires
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	
	/**
	  * @param id A SessionToken id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param loggedOut Time when this session was ended due to the user logging out. None if not logged out.
	  * @return A model containing only the specified loggedOut
	  */
	def withLoggedOut(loggedOut: Instant) = apply(loggedOut = Some(loggedOut))
	
	/**
	  * @param modelStylePreference Model style preferred during this session
	  * @return A model containing only the specified modelStylePreference
	  */
	def withModelStylePreference(modelStylePreference: ModelStyle) = 
		apply(modelStylePreference = Some(modelStylePreference))
	
	/**
	  * @param token Textual representation of this token
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
	
	/**
	  * @param userId Id of the user who owns this token
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with SessionTokens in the database
  * @param id SessionToken database id
  * @param userId Id of the user who owns this token
  * @param token Textual representation of this token
  * @param expires Time when this token expires
  * @param deviceId Id of the device on which this session is, if applicable
  * @param modelStylePreference Model style preferred during this session
  * @param created Time when this session was started
  * @param loggedOut Time when this session was ended due to the user logging out. None if not logged out.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class SessionTokenModel(id: Option[Int] = None, userId: Option[Int] = None, 
	token: Option[String] = None, expires: Option[Instant] = None, deviceId: Option[Int] = None, 
	modelStylePreference: Option[ModelStyle] = None, created: Option[Instant] = None, 
	loggedOut: Option[Instant] = None) 
	extends StorableWithFactory[SessionToken]
{
	// IMPLEMENTED	--------------------
	
	override def factory = SessionTokenModel.factory
	
	override def valueProperties = 
	{
		import SessionTokenModel._
		Vector("id" -> id, userIdAttName -> userId, tokenAttName -> token, expiresAttName -> expires, 
			deviceIdAttName -> deviceId, modelStylePreferenceAttName -> modelStylePreference.map { _.id }, 
			createdAttName -> created, loggedOutAttName -> loggedOut)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deviceId A new deviceId
	  * @return A new copy of this model with the specified deviceId
	  */
	def withDeviceId(deviceId: Int) = copy(deviceId = Some(deviceId))
	
	/**
	  * @param expires A new expires
	  * @return A new copy of this model with the specified expires
	  */
	def withExpires(expires: Instant) = copy(expires = Some(expires))
	
	/**
	  * @param loggedOut A new loggedOut
	  * @return A new copy of this model with the specified loggedOut
	  */
	def withLoggedOut(loggedOut: Instant) = copy(loggedOut = Some(loggedOut))
	
	/**
	  * @param modelStylePreference A new modelStylePreference
	  * @return A new copy of this model with the specified modelStylePreference
	  */
	def withModelStylePreference(modelStylePreference: ModelStyle) = 
		copy(modelStylePreference = Some(modelStylePreference))
	
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

