package utopia.exodus.model.stored

import utopia.exodus.model.partial.UserSessionData
import utopia.metropolis.model.stored.Stored

/**
  * Represents a user session that has been stored to the DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v2
  */
case class UserSession(id: Int, data: UserSessionData) extends Stored[UserSessionData]
