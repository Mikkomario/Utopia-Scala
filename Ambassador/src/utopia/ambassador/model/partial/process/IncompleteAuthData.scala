package utopia.ambassador.model.partial.process

import utopia.flow.time.Now

import java.time.Instant

/**
  * Contains information about authentication attempts that were started from a
  * 3rd party service and not in this service
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param serviceId Id of the service targeted by this authentication
  * @param code Authentication code provided by the 3rd party service
  * @param token Token used for authenticating the response to this case
  * @param expiration Time when the authentication token expires
  * @param created Time when this case was opened (default = Now)
  */
case class IncompleteAuthData(serviceId: Int, code: String, token: String, expiration: Instant,
                              created: Instant = Now)
