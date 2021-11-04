package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleEmailValidationAttempt
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a EmailValidationAttempt that has already been stored in the database
  * @param id id of this EmailValidationAttempt in the database
  * @param data Wrapped EmailValidationAttempt data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class EmailValidationAttempt(id: Int, data: EmailValidationAttemptData) 
	extends StoredModelConvertible[EmailValidationAttemptData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this EmailValidationAttempt in the database
	  */
	def access = DbSingleEmailValidationAttempt(id)
}

