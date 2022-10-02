package utopia.exodus.database.access.many.auth

import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target multiple email validation attempts or similar instances
  *  at a time
  * @author Mikko Hilpinen
  * @since 21.02.2022, v4.0
  */
trait ManyEmailValidationAttemptsAccessLike[+A, +Repr <: ManyModelAccess[A]] 
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
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
	  * purpose ids of the accessible email validation attempts
	  */
	def purposeIds(implicit connection: Connection) = pullColumn(model.purposeIdColumn).map { v => v.getInt }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the email addresses of the targeted email validation attempts
	  * @param newEmailAddress A new email address to assign
	  * @return Whether any email validation attempt was affected
	  */
	def emailAddresses_=(newEmailAddress: String)(implicit connection: Connection) = 
		putColumn(model.emailAddressColumn, newEmailAddress)
	
	/**
	  * Updates the purpose ids of the targeted email validation attempts
	  * @param newPurposeId A new purpose id to assign
	  * @return Whether any email validation attempt was affected
	  */
	def purposeIds_=(newPurposeId: Int)(implicit connection: Connection) = 
		putColumn(model.purposeIdColumn, newPurposeId)
	
	/**
	  * Updates the tokens ids of the targeted email validation attempts
	  * @param newTokenId A new token id to assign
	  * @return Whether any email validation attempt was affected
	  */
	def tokensIds_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn, 
		newTokenId)
}

