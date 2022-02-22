package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.EmailValidationResend
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual EmailValidationResends, based on their id
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class DbSingleEmailValidationResend(id: Int) 
	extends UniqueEmailValidationResendAccess with SingleIntIdModelAccess[EmailValidationResend]

