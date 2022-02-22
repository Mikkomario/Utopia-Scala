package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleEmailValidationAttempt
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a email validation attempt that has already been stored in the database
  * @param id id of this email validation attempt in the database
  * @param data Wrapped email validation attempt data
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class EmailValidationAttempt(id: Int, data: EmailValidationAttemptData) 
	extends StoredModelConvertible[EmailValidationAttemptData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this email validation attempt in the database
	  */
	def access = DbSingleEmailValidationAttempt(id)
}

