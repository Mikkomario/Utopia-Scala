package utopia.ambassador.model.cached

import utopia.disciple.apache.Gateway

import scala.concurrent.duration.Duration

/**
  * Specifies settings to use when acquiring new access tokens
  * @author Mikko Hilpinen
  * @since 20.7.2021, v1.0
  * @param gateway Gateway instance to make requests with.
  * @param refreshTokenDuration Duration how long to keep refresh tokens valid (default = infinite)
  * @param useAuthorizationHeader Whether client id and client secret should be sent
  *                               in a basic auth header (true) or in the request body (false, default)
  */
case class TokenInterfaceConfiguration(gateway: Gateway, refreshTokenDuration: Duration = Duration.Inf,
                                       useAuthorizationHeader: Boolean = false)
