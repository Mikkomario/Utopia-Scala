package utopia.exodus.model.partial.auth

import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext
import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection

/**
  * Used for authenticating temporary user sessions
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
case class SessionTokenData(userId: Int, token: String, expires: Instant, deviceId: Option[Int] = None, 
	modelStylePreference: Option[ModelStyle], created: Instant = Now, loggedOut: Option[Instant] = None) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this SessionToken has already been deprecated
	  */
	def isDeprecated = loggedOut.isDefined
	/**
	  * Whether this SessionToken is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	/**
	  * @param context Implicit request context
	  * @return The model style to use during a request handling, based on either a header (X-Style) value,
	  * a query parameter (style) value, session default or session type
	  */
	def modelStyle(implicit context: AuthorizedContext) =
		context.modelStyle.orElse(modelStylePreference).getOrElse { ExodusContext.defaultModelStyle }
	/**
	  * @param context Implicit request context
	  * @param connection Implicit database connection (used for reading language ids from DB if necessary)
	  * @return Language id list to use during this session - from most to least preferred
	  */
	def languageIds(implicit context: AuthorizedContext, connection: Connection) = context.languageIds(userId)
	
	/**
	  * @return An access point to this session's user's data in the DB
	  */
	def userAccess = DbUser(userId)
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("user_id" -> userId, "token" -> token, "expires" -> expires, "device_id" -> deviceId, 
			"model_style_preference" -> modelStylePreference.map { _.id }, "created" -> created, 
			"logged_out" -> loggedOut))
}

