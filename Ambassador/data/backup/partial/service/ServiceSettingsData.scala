package utopia.ambassador.model.partial.service

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
  * Contains service-specific settings
  * @author Mikko Hilpinen
  * @since 14.7.2021, v1.0
  * @param serviceId Id of the described 3rd party service
  * @param clientId Client id issued for this service by the 3rd party service
  * @param clientSecret Client secret given for this service to authenticate requests to the 3rd party service
  * @param authenticationUrl Url address whether users will be redirected for authentication
  * @param tokenUrl Url address where this service can request for session and refresh tokens
  * @param redirectUrl Url within this service that receives user redirects from the 3rd party service
  * @param incompleteAuthUrl Url on the client side that will receive cases where an authentication process is
  *                          initiated in the 3rd party service and hasn't been connected to any user account yet.
  *                          None if that use case is not supported.
  * @param defaultCompletionUrl Default url address on client side where users will be redirected after authentication
  *                             process completes. Used when the client hasn't provided a redirection url that covers
  *                             the case in question. None if all clients must always specify their own redirect urls.
  * @param preparationTokenDuration Maximum duration within which the clients must request authentication redirection
  *                                 for the user after they have prepared such a redirection (default = 5 minutes)
  * @param redirectTokenDuration Maximum duration within which the redirected user must be received back to this
  *                              service in order for the authentication to be considered valid (default = 15 minutes)
  * @param incompleteAuthTokenDuration Maximum duration within which the user must log in or create a user account
  *                                    in cases where they arrive from a 3rd party service (default = 30 minutes)
  * @param defaultSessionDuration Assumed authentication token validity time when no time is specified in the
  *                               authentication response by the 3rd party service (default = 22 hours)
  * @param created Creation time of these settings
  */
case class ServiceSettingsData(serviceId: Int, clientId: String, clientSecret: String,
                               authenticationUrl: String, tokenUrl: String, redirectUrl: String,
                               incompleteAuthUrl: Option[String] = None, defaultCompletionUrl: Option[String] = None,
                               preparationTokenDuration: FiniteDuration = 5.minutes,
                               redirectTokenDuration: FiniteDuration = 15.minutes,
                               incompleteAuthTokenDuration: FiniteDuration = 30.minutes,
                               defaultSessionDuration: FiniteDuration = 22.hours, created: Instant = Now)
