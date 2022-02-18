package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleEmailValidationPurpose
import utopia.exodus.model.partial.auth.EmailValidationPurposeData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a EmailValidationPurpose that has already been stored in the database
  * @param id id of this EmailValidationPurpose in the database
  * @param data Wrapped EmailValidationPurpose data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class EmailValidationPurpose(id: Int, data: EmailValidationPurposeData) 
	extends StoredModelConvertible[EmailValidationPurposeData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this EmailValidationPurpose in the database
	  */
	def access = DbSingleEmailValidationPurpose(id)
}

