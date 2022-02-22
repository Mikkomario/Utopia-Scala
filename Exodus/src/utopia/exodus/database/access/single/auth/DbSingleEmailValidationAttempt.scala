package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual email validation attempts, based on their id
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class DbSingleEmailValidationAttempt(id: Int) 
	extends UniqueEmailValidationAttemptAccess with SingleIntIdModelAccess[EmailValidationAttempt]

