package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleEmailValidationResend
import utopia.exodus.model.partial.auth.EmailValidationResendData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a EmailValidationResend that has already been stored in the database
  * @param id id of this EmailValidationResend in the database
  * @param data Wrapped EmailValidationResend data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class EmailValidationResend(id: Int, data: EmailValidationResendData) 
	extends StoredModelConvertible[EmailValidationResendData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this EmailValidationResend in the database
	  */
	def access = DbSingleEmailValidationResend(id)
}

