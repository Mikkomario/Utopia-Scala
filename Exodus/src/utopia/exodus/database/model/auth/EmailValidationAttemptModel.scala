package utopia.exodus.database.model.auth

import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing EmailValidationAttemptModel instances and for inserting email validation attempts
  *  to the database
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object EmailValidationAttemptModel 
	extends DataInserter[EmailValidationAttemptModel, EmailValidationAttempt, EmailValidationAttemptData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains email validation attempt token id
	  */
	val tokenIdAttName = "tokenId"
	
	/**
	  * Name of the property that contains email validation attempt email address
	  */
	val emailAddressAttName = "emailAddress"
	
	/**
	  * Name of the property that contains email validation attempt purpose id
	  */
	val purposeIdAttName = "purposeId"
	
	/**
	  * Name of the property that contains email validation attempt resend token hash
	  */
	val resendTokenHashAttName = "resendTokenHash"
	
	/**
	  * Name of the property that contains email validation attempt send count
	  */
	val sendCountAttName = "sendCount"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains email validation attempt token id
	  */
	def tokenIdColumn = table(tokenIdAttName)
	
	/**
	  * Column that contains email validation attempt email address
	  */
	def emailAddressColumn = table(emailAddressAttName)
	
	/**
	  * Column that contains email validation attempt purpose id
	  */
	def purposeIdColumn = table(purposeIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = EmailValidationAttemptFactory
	
	/**
	  * Column that contains email validation attempt resend token hash
	  */
	def resendTokenHashColumn = table(resendTokenHashAttName)
	
	/**
	  * Column that contains email validation attempt send count
	  */
	def sendCountColumn = table(sendCountAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: EmailValidationAttemptData) = 
		apply(None, Some(data.tokenId), Some(data.emailAddress), Some(data.purposeId))
	
	override def complete(id: Value, data: EmailValidationAttemptData) = EmailValidationAttempt(id.getInt, 
		data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param emailAddress Address to which the validation email was sent
	  * @return A model containing only the specified email address
	  */
	def withEmailAddress(emailAddress: String) = apply(emailAddress = Some(emailAddress))
	
	/**
	  * @param id A email validation attempt id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param purposeId Id of the purpose this email validation is for
	  * @return A model containing only the specified purpose id
	  */
	def withPurposeId(purposeId: Int) = apply(purposeId = Some(purposeId))
	
	/**
	  * @param resendTokenHash Hashed token which may be used to send a copy of this email validation. None if
	  * resend is disabled.
	  * @return A model containing only the specified resend token hash
	  */
	def withResendTokenHash(resendTokenHash: String) = apply(resendTokenHash = Some(resendTokenHash))
	
	/**
	  * 
		@param sendCount Number of times a validation email has been sent for this specific purpose up to this point.
	  * @return A model containing only the specified send count
	  */
	def withSendCount(sendCount: Int) = apply(sendCount = Some(sendCount))
	
	/**
	  * @param tokenId Id of the token sent via email
	  * @return A model containing only the specified token id
	  */
	def withTokenId(tokenId: Int) = apply(tokenId = Some(tokenId))
}

/**
  * Used for interacting with EmailValidationAttempts in the database
  * @param id email validation attempt database id
  * @param tokenId Id of the token sent via email
  * @param emailAddress Address to which the validation email was sent
  * @param purposeId Id of the purpose this email validation is for
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class EmailValidationAttemptModel(id: Option[Int] = None, tokenId: Option[Int] = None, 
	emailAddress: Option[String] = None, purposeId: Option[Int] = None) 
	extends StorableWithFactory[EmailValidationAttempt]
{
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptModel.factory
	
	override def valueProperties = {
		import EmailValidationAttemptModel._
		Vector("id" -> id, tokenIdAttName -> tokenId, emailAddressAttName -> emailAddress, 
			purposeIdAttName -> purposeId)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param emailAddress A new email address
	  * @return A new copy of this model with the specified email address
	  */
	def withEmailAddress(emailAddress: String) = copy(emailAddress = Some(emailAddress))
	
	/**
	  * @param purposeId A new purpose id
	  * @return A new copy of this model with the specified purpose id
	  */
	def withPurposeId(purposeId: Int) = copy(purposeId = Some(purposeId))
	
	/**
	  * @param resendTokenHash A new resend token hash
	  * @return A new copy of this model with the specified resend token hash
	  */
	def withResendTokenHash(resendTokenHash: String) = copy(resendTokenHash = Some(resendTokenHash))
	
	/**
	  * @param sendCount A new send count
	  * @return A new copy of this model with the specified send count
	  */
	def withSendCount(sendCount: Int) = copy(sendCount = Some(sendCount))
	
	/**
	  * @param tokenId A new token id
	  * @return A new copy of this model with the specified token id
	  */
	def withTokenId(tokenId: Int) = copy(tokenId = Some(tokenId))
}

