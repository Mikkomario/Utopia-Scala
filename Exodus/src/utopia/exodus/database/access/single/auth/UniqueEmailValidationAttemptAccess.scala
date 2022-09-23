package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.collection.value.typeless.Value
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
	  * Id of the purpose this email validation is for. None if no instance (or value) was found.
	  */
	def purposeId(implicit connection: Connection) = pullColumn(model.purposeIdColumn).int
	
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
	  * Updates the purpose ids of the targeted email validation attempts
	  * @param newPurposeId A new purpose id to assign
	  * @return Whether any email validation attempt was affected
	  */
	def purposeId_=(newPurposeId: Int)(implicit connection: Connection) = 
		putColumn(model.purposeIdColumn, newPurposeId)
	
	/**
	  * Updates the tokens ids of the targeted email validation attempts
	  * @param newTokenId A new token id to assign
	  * @return Whether any email validation attempt was affected
	  */
	def tokenId_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn, 
		newTokenId)
}

