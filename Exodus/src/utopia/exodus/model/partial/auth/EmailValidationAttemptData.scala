package utopia.exodus.model.partial.auth

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible

/**
  * Represents an attempted email validation. Provides additional information to an authentication token.
  * @param tokenId Id of the token sent via email
  * @param emailAddress Address to which the validation email was sent
  * @param purposeId Id of the purpose this email validation is for
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class EmailValidationAttemptData(tokenId: Int, emailAddress: String, purposeId: Int) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("token_id" -> tokenId, "email_address" -> emailAddress, "purpose_id" -> purposeId))
}

