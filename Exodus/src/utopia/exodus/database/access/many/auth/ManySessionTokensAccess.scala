package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.SessionTokenFactory
import utopia.exodus.database.model.auth.SessionTokenModel
import utopia.exodus.model.stored.auth.SessionToken
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManySessionTokensAccess
{
	// NESTED	--------------------
	
	private class ManySessionTokensSubView(override val parent: ManyRowModelAccess[SessionToken], 
		override val filterCondition: Condition) 
		extends ManySessionTokensAccess with SubView
}

/**
  * A common trait for access points which target multiple SessionTokens at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
trait ManySessionTokensAccess extends ManyRowModelAccess[SessionToken] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * userIds of the accessible SessionTokens
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	/**
	  * tokens of the accessible SessionTokens
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	/**
	  * expirationTimes of the accessible SessionTokens
	  */
	def expirationTimes(implicit connection: Connection) = 
		pullColumn(model.expiresColumn).flatMap { value => value.instant }
	/**
	  * deviceIds of the accessible SessionTokens
	  */
	def deviceIds(implicit connection: Connection) = 
		pullColumn(model.deviceIdColumn).flatMap { value => value.int }
	/**
	  * modelStylePreferences of the accessible SessionTokens
	  */
	def modelStylePreferences(implicit connection: Connection) = 
		pullColumn(model.modelStylePreferenceColumn)
			.flatMap { value => value.int.flatMap(ModelStyle.findForId) }
	/**
	  * creationTimes of the accessible SessionTokens
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	/**
	  * logoutTimes of the accessible SessionTokens
	  */
	def logoutTimes(implicit connection: Connection) = 
		pullColumn(model.loggedOutColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SessionTokenModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SessionTokenFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManySessionTokensAccess = 
		new ManySessionTokensAccess.ManySessionTokensSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param deviceId Id of the targeted device
	  * @return An access point to session tokens that are linked with that device
	  */
	def onDeviceWithId(deviceId: Int) = filter(model.withDeviceId(deviceId).toCondition)
	
	/**
	  * Logs out from all accessible sessions, deprecating accessible tokens
	  * @param connection Implicit DB Connection
	  * @return Whether any session token was affected
	  */
	def logOut()(implicit connection: Connection) = logoutTimes = Now
	
	/**
	  * Updates the created of the targeted SessionToken instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the deviceId of the targeted SessionToken instance(s)
	  * @param newDeviceId A new deviceId to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def deviceIds_=(newDeviceId: Int)(implicit connection: Connection) = 
		putColumn(model.deviceIdColumn, newDeviceId)
	/**
	  * Updates the expires of the targeted SessionToken instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	/**
	  * Updates the loggedOut of the targeted SessionToken instance(s)
	  * @param newLoggedOut A new loggedOut to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def logoutTimes_=(newLoggedOut: Instant)(implicit connection: Connection) = 
		putColumn(model.loggedOutColumn, newLoggedOut)
	/**
	  * Updates the modelStylePreference of the targeted SessionToken instance(s)
	  * @param newModelStylePreference A new modelStylePreference to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def modelStylePreferences_=(newModelStylePreference: ModelStyle)(implicit connection: Connection) = 
		putColumn(model.modelStylePreferenceColumn, newModelStylePreference.id)
	/**
	  * Updates the token of the targeted SessionToken instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	/**
	  * Updates the userId of the targeted SessionToken instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any SessionToken instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

