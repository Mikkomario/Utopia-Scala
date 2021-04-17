package utopia.exodus.model.stored

import utopia.exodus.model.partial.EmailValidationData
import utopia.metropolis.model.stored.Stored

/**
  * Represents a stored email validation record
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
case class EmailValidation(id: Int, data: EmailValidationData) extends Stored[EmailValidationData]
