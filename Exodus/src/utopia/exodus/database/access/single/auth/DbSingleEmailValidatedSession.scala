package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.EmailValidatedSession
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual EmailValidatedSessions, based on their id
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
@deprecated("Will be removed in a future release", "v4.0")
case class DbSingleEmailValidatedSession(id: Int) 
	extends UniqueEmailValidatedSessionAccess with SingleIntIdModelAccess[EmailValidatedSession]

