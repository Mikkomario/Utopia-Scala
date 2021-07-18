package utopia.ambassador.model.partial.process

import utopia.flow.time.Now

import java.time.Instant

/**
  * Contains information about an event where a user was redirected to a 3rd party authentication service
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param preparationId Id of the preparation of this redirect
  * @param token Authentication token associated with this redirection
  * @param expiration Expiration time for the authentication token
  * @param created Creation time of this event (default = Now)
  */
case class AuthUserRedirectData(preparationId: Int, token: String, expiration: Instant, created: Instant = Now)
