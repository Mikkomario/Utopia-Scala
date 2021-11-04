package utopia.exodus.model.partial

import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}

import java.time.Instant

/**
  * Contains basic information about a temporary user session
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  * @param userId Id of the user who owns this session key
  * @param key The session key
  * @param expires Time threshold when this key is no longer usable
  * @param deviceId Id of the device that uses this session key. None if this session is not for any specific
  *                 device (browser quest mode) (default)
  * @param preferredModelStyle Model style preferred by this user during this session (optional)
  */
case class UserSessionData(userId: Int, key: String, expires: Instant, deviceId: Option[Int] = None,
                           preferredModelStyle: Option[ModelStyle] = None)
{
	/**
	  * The model style to use during a request handling, based on either a header (X-Style) value,
	  * a query parameter (style) value, session default or session type
	  * @param context Implicit request context
	  * @return Model style to use
	  */
	def modelStyle(implicit context: AuthorizedContext) =
		context.modelStyle.orElse(preferredModelStyle).getOrElse { if (deviceId.isDefined) Full else Simple }
}
