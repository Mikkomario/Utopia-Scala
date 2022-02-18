package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleEmailValidationAttemptOld
import utopia.exodus.model.partial.auth.EmailValidationAttemptDataOld
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a EmailValidationAttempt that has already been stored in the database
  * @param id id of this EmailValidationAttempt in the database
  * @param data Wrapped EmailValidationAttempt data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Replaced with a new version", "v4.0")
case class EmailValidationAttemptOld(id: Int, data: EmailValidationAttemptDataOld)
	extends StoredModelConvertible[EmailValidationAttemptDataOld]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this EmailValidationAttempt in the database
	  */
	def access = DbSingleEmailValidationAttemptOld(id)
}

