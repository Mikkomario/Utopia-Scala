package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyEmailValidationAttemptsAccess
{
	// NESTED	--------------------
	
	private class ManyEmailValidationAttemptsSubView(override val parent: ManyRowModelAccess[EmailValidationAttempt], 
		override val filterCondition: Condition) 
		extends ManyEmailValidationAttemptsAccess with SubView
}

/**
  * A common trait for access points which target multiple email validation attempts at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyEmailValidationAttemptsAccess 
	extends ManyRowModelAccess[EmailValidationAttempt] with FilterableView[ManyEmailValidationAttemptsAccess] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * tokens ids of the accessible email validation attempts
	  */
	def tokensIds(implicit connection: Connection) = pullColumn(model.tokenIdColumn).map { v => v.getInt }
	
	/**
	  * email addresses of the accessible email validation attempts
	  */
	def emailAddresses(implicit connection: Connection) = 
		pullColumn(model.emailAddressColumn).map { v => v.getString }
	
	/**
	  * resend token hashes of the accessible email validation attempts
	  */
	def resendTokenHashes(implicit connection: Connection) = 
		pullColumn(model.resendTokenHashColumn).flatMap { _.string }
	
	/**
	  * send counts of the accessible email validation attempts
	  */
	def sendCounts(implicit connection: Connection) = pullColumn(model.sendCountColumn).map { v => v.getInt }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactory
	
	override def filter(additionalCondition: Condition): ManyEmailValidationAttemptsAccess = 
		new ManyEmailValidationAttemptsAccess.ManyEmailValidationAttemptsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the email addresses of the targeted email validation attempts
	  * @param newEmailAddress A new email address to assign
	  * @return Whether any email validation attempt was affected
	  */
	def emailAddresses_=(newEmailAddress: String)(implicit connection: Connection) = 
		putColumn(model.emailAddressColumn, newEmailAddress)
	
	/**
	  * Updates the resend token hashes of the targeted email validation attempts
	  * @param newResendTokenHash A new resend token hash to assign
	  * @return Whether any email validation attempt was affected
	  */
	def resendTokenHashes_=(newResendTokenHash: String)(implicit connection: Connection) = 
		putColumn(model.resendTokenHashColumn, newResendTokenHash)
	
	/**
	  * Updates the send counts of the targeted email validation attempts
	  * @param newSendCount A new send count to assign
	  * @return Whether any email validation attempt was affected
	  */
	def sendCounts_=(newSendCount: Int)(implicit connection: Connection) = 
		putColumn(model.sendCountColumn, newSendCount)
	
	/**
	  * Updates the tokens ids of the targeted email validation attempts
	  * @param newTokenId A new token id to assign
	  * @return Whether any email validation attempt was affected
	  */
	def tokensIds_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn, 
		newTokenId)
}

