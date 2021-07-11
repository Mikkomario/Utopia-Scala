package utopia.ambassador.model.partial.token

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import java.time.Instant

/**
  * Contains information about an authorization token
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  * @param userId Id of the user this token is bound to
  * @param token String value of this token
  * @param created Creation time of this token (default = now)
  * @param expiration Expiration time of this token, if this is a temporary token (default = None)
  * @param deprecatedAfter Time when this token was deprecated / revoked / replaced. None if still valid (default)
  * @param isRefreshToken Whether this token can be used to request more access tokens (default = false)
  */
case class AuthTokenData(userId: Int, token: String, created: Instant = Now, expiration: Option[Instant] = None,
                         deprecatedAfter: Option[Instant] = None, isRefreshToken: Boolean)
{
	/**
	  * @return Whether this is a temporary token (will expire)
	  */
	def isTemporary = expiration.isDefined
	/**
	  * @return Whether this token has expired or been deprecated
	  */
	def hasExpired = deprecatedAfter.isDefined || expiration.exists { _.isInPast }
}
