package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleEmailValidationPurpose
import utopia.exodus.model.partial.auth.EmailValidationPurposeData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a email validation purpose that has already been stored in the database
  * @param id id of this email validation purpose in the database
  * @param data Wrapped email validation purpose data
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
case class EmailValidationPurpose(id: Int, data: EmailValidationPurposeData) 
	extends StoredModelConvertible[EmailValidationPurposeData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this email validation purpose in the database
	  */
	def access = DbSingleEmailValidationPurpose(id)
}

