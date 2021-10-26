package utopia.ambassador.model.partial.process

import utopia.flow.time.Now

import java.time.Instant

/**
  * Contains information about incomplete authentication closings,
  * which are created when the authenticated user logs in to the service
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param authenticationId Id of the incomplete authentication this login completes
  * @param userId Id of the authenticated user
  * @param created Creation time of this result
  * @param wasSuccess Whether access token(s) were successfully acquired
  */
case class IncompleteAuthLoginData(authenticationId: Int, userId: Int, created: Instant = Now,
                                   wasSuccess: Boolean = false)
