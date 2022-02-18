package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct email validation attempts.
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait UniqueEmailValidationAttemptAccess 
	extends SingleRowModelAccess[EmailValidationAttempt] 
		with DistinctModelAccess[EmailValidationAttempt, Option[EmailValidationAttempt], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the token sent via email. None if no instance (or value) was found.
	  */
	def tokenId(implicit connection: Connection) = pullColumn(model.tokenIdColumn).int
	
	/**
	  * Address to which the validation email was sent. None if no instance (or value) was found.
	  */
	def emailAddress(implicit connection: Connection) = pullColumn(model.emailAddressColumn).string
	
	/**
	  * 
		Hashed token which may be used to send a copy of this email validation. None if resend is disabled.. None if
	  *  no instance (or value) was found.
	  */
	def resendTokenHash(implicit connection: Connection) = pullColumn(model.resendTokenHashColumn).string
	
	/**
	  * Number of times a validation email has been sent for this specific purpose up to this point.. None if
	  *  no instance (or value) was found.
	  */
	def sendCount(implicit connection: Connection) = pullColumn(model.sendCountColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the email addresses of the targeted email validation attempts
	  * @param newEmailAddress A new email address to assign
	  * @return Whether any email validation attempt was affected
	  */
	def emailAddress_=(newEmailAddress: String)(implicit connection: Connection) = 
		putColumn(model.emailAddressColumn, newEmailAddress)
	
	/**
	  * Updates the resend token hashes of the targeted email validation attempts
	  * @param newResendTokenHash A new resend token hash to assign
	  * @return Whether any email validation attempt was affected
	  */
	def resendTokenHash_=(newResendTokenHash: String)(implicit connection: Connection) = 
		putColumn(model.resendTokenHashColumn, newResendTokenHash)
	
	/**
	  * Updates the send counts of the targeted email validation attempts
	  * @param newSendCount A new send count to assign
	  * @return Whether any email validation attempt was affected
	  */
	def sendCount_=(newSendCount: Int)(implicit connection: Connection) = 
		putColumn(model.sendCountColumn, newSendCount)
	
	/**
	  * Updates the tokens ids of the targeted email validation attempts
	  * @param newTokenId A new token id to assign
	  * @return Whether any email validation attempt was affected
	  */
	def tokenId_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn, 
		newTokenId)
}

