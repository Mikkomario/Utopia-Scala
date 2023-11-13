package utopia.ambassador.model.partial.token

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

/**
  * Tokens (both access and refresh) used for authenticating 3rd party requests
  * @param userId Id of the user who owns this token / to whom this token is linked
  * @param token Textual representation of this token
  * @param expires Time when this token can no longer be used, if applicable
  * @param created Time when this token was acquired / issued
  * @param deprecatedAfter Time when this token was cancelled, revoked or replaced
  * @param isRefreshToken Whether this is a refresh token which can be used for acquiring access tokens
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthTokenData(userId: Int, token: String, expires: Option[Instant] = None, created: Instant = Now, 
	deprecatedAfter: Option[Instant] = None, isRefreshToken: Boolean = false) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this AuthToken has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined || expires.exists { _.isPast }
	/**
	  * Whether this AuthToken is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	/**
	  * @return Whether this is a session / access token and not a refresh token
	  */
	def isSessionToken = !isRefreshToken
	
	/**
	  * @return Whether this is a temporary token (will expire)
	  */
	def isTemporary = expires.isDefined
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("user_id" -> userId, "token" -> token, "expires" -> expires, "created" -> created, 
			"deprecated_after" -> deprecatedAfter, "is_refresh_token" -> isRefreshToken))
}

