package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.SessionTokenFactory
import utopia.exodus.database.model.auth.SessionTokenModel
import utopia.exodus.model.stored.auth.SessionToken
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct SessionTokens.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
trait UniqueSessionTokenAccess 
	extends SingleRowModelAccess[SessionToken] 
		with DistinctModelAccess[SessionToken, Option[SessionToken], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the user who owns this token. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	/**
	  * Textual representation of this token. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	/**
	  * Time when this token expires. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	/**
	  * Id of the device on which this session is, if applicable. None if no instance (or value) was found.
	  */
	def deviceId(implicit connection: Connection) = pullColumn(model.deviceIdColumn).int
	/**
	  * Model style preferred during this session. None if no instance (or value) was found.
	  */
	def modelStylePreference(implicit connection: Connection) = 
		pullColumn(model.modelStylePreferenceColumn).int.flatMap(ModelStyle.findForId)
	/**
	  * Time when this session was started. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	/**
	  * Time when this session was ended due to the user logging out. None if not logged out.. None if no instance (or value) was found.
	  */
	def loggedOut(implicit connection: Connection) = pullColumn(model.loggedOutColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SessionTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SessionTokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Deprecates this token by logging out / ending the current session
	  * @param connection Implicit DB Connection
	  * @return Whether a token was affected
	  */
	def logOut()(implicit connection: Connection) = loggedOut = Now
	
	/**
	  * Updates the created of the targeted SessionToken instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the deviceId of the targeted SessionToken instance(s)
	  * @param newDeviceId A new deviceId to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def deviceId_=(newDeviceId: Int)(implicit connection: Connection) = 
		putColumn(model.deviceIdColumn, newDeviceId)
	/**
	  * Updates the expires of the targeted SessionToken instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	/**
	  * Updates the loggedOut of the targeted SessionToken instance(s)
	  * @param newLoggedOut A new loggedOut to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def loggedOut_=(newLoggedOut: Instant)(implicit connection: Connection) = 
		putColumn(model.loggedOutColumn, newLoggedOut)
	/**
	  * Updates the modelStylePreference of the targeted SessionToken instance(s)
	  * @param newModelStylePreference A new modelStylePreference to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def modelStylePreference_=(newModelStylePreference: ModelStyle)(implicit connection: Connection) = 
		putColumn(model.modelStylePreferenceColumn, newModelStylePreference.id)
	/**
	  * Updates the token of the targeted SessionToken instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	/**
	  * Updates the userId of the targeted SessionToken instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

