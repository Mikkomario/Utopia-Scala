package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Represents a time when an email validation was sent again
  * @param validationId Id of the email_validation_attempt linked with this EmailValidationResend
  * @param created Time when this EmailValidationResend was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class EmailValidationResendData(validationId: Int, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("validation_id" -> validationId, "created" -> created))
}

