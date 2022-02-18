package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual EmailValidationPurposes, based on their id
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class DbSingleEmailValidationPurpose(id: Int) 
	extends UniqueEmailValidationPurposeAccess with SingleIntIdModelAccess[EmailValidationPurpose]

