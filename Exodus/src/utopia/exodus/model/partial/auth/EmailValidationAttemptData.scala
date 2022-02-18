package utopia.exodus.model.partial.auth

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Represents an attempted email validation. Provides additional information to an authentication token.
  * @param tokenId Id of the token sent via email
  * @param emailAddress Address to which the validation email was sent
  * @param resendTokenHash Hashed token which may be used to send a copy of this email validation. None if
  *  resend is disabled.
  * 
	@param sendCount Number of times a validation email has been sent for this specific purpose up to this point.
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class EmailValidationAttemptData(tokenId: Int, emailAddress: String, 
	resendTokenHash: Option[String] = None, sendCount: Int = 1) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("token_id" -> tokenId, "email_address" -> emailAddress, 
			"resend_token_hash" -> resendTokenHash, "send_count" -> sendCount))
}

