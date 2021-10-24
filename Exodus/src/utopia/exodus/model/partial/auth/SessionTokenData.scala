package utopia.exodus.model.partial.auth

import utopia.exodus.rest.util.AuthorizedContext

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}

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
	  * The model style to use during a request handling, based on either a header (X-Style) value,
	  * a query parameter (style) value, session default or session type
	  * @param context Implicit request context
	  * @return Model style to use
	  */
	def modelStyle(implicit context: AuthorizedContext) =
		context.modelStyle.orElse(modelStylePreference).getOrElse { if (deviceId.isDefined) Full else Simple }
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("user_id" -> userId, "token" -> token, "expires" -> expires, "device_id" -> deviceId, 
			"model_style_preference" -> modelStylePreference.map { _.id }, "created" -> created, 
			"logged_out" -> loggedOut))
}

