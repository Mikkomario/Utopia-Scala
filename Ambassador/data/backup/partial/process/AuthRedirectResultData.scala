package utopia.ambassador.model.partial.process

import utopia.ambassador.model.enumeration.GrantLevel
import utopia.flow.time.Now

import java.time.Instant

/**
  * Contains information about the result of an authentication redirection
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param redirectId Id of the redirect attempt this result answers to
  * @param grantLevel Access level granted by the user
  * @param created Creation time of this result
  */
case class AuthRedirectResultData(redirectId: Int, grantLevel: GrantLevel, created: Instant = Now)
