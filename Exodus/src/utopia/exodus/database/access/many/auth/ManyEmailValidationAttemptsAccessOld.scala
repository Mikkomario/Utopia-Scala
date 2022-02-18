package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationAttemptFactoryOld
import utopia.exodus.database.model.auth.EmailValidationAttemptModelOld
import utopia.exodus.model.stored.auth.EmailValidationAttemptOld
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyEmailValidationAttemptsAccessOld
{
	// NESTED	--------------------
	
	private class ManyEmailValidationAttemptsSubViewOld(override val parent: ManyRowModelAccess[EmailValidationAttemptOld],
	                                                    override val filterCondition: Condition)
		extends ManyEmailValidationAttemptsAccessOld with SubView
}

/**
  * A common trait for access points which target multiple EmailValidationAttempts at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Replaced with a new version", "v4.0")
trait ManyEmailValidationAttemptsAccessOld
	extends ManyRowModelAccess[EmailValidationAttemptOld] with Indexed
		with FilterableView[ManyEmailValidationAttemptsAccessOld]
{
	// COMPUTED	--------------------
	
	/**
	  * purposeIds of the accessible EmailValidationAttempts
	  */
	def purposeIds(implicit connection: Connection) =
		pullColumn(model.purposeIdColumn).flatMap { value => value.int }
	
	/**
	  * emailAddresses of the accessible EmailValidationAttempts
	  */
	def emailAddresses(implicit connection: Connection) =
		pullColumn(model.emailColumn).flatMap { value => value.string }
	
	/**
	  * tokens of the accessible EmailValidationAttempts
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	
	/**
	  * resendTokens of the accessible EmailValidationAttempts
	  */
	def resendTokens(implicit connection: Connection) =
		pullColumn(model.resendTokenColumn).flatMap { value => value.string }
	
	/**
	  * userIds of the accessible EmailValidationAttempts
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * expirationTimes of the accessible EmailValidationAttempts
	  */
	def expirationTimes(implicit connection: Connection) =
		pullColumn(model.expiresColumn).flatMap { value => value.instant }
	
	/**
	  * creationTimes of the accessible EmailValidationAttempts
	  */
	def creationTimes(implicit connection: Connection) =
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	/**
	  * completionTimes of the accessible EmailValidationAttempts
	  */
	def completionTimes(implicit connection: Connection) =
		pullColumn(model.completedColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModelOld
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactoryOld
	
	override def filter(additionalCondition: Condition): ManyEmailValidationAttemptsAccessOld =
		new ManyEmailValidationAttemptsAccessOld.ManyEmailValidationAttemptsSubViewOld(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the completed of the targeted EmailValidationAttempt instance(s)
	  * @param newCompleted A new completed to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def completionTimes_=(newCompleted: Instant)(implicit connection: Connection) = 
		putColumn(model.completedColumn, newCompleted)
	
	/**
	  * Updates the created of the targeted EmailValidationAttempt instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the email of the targeted EmailValidationAttempt instance(s)
	  * @param newEmail A new email to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def emailAddresses_=(newEmail: String)(implicit connection: Connection) = 
		putColumn(model.emailColumn, newEmail)
	
	/**
	  * Updates the expires of the targeted EmailValidationAttempt instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the purposeId of the targeted EmailValidationAttempt instance(s)
	  * @param newPurposeId A new purposeId to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def purposeIds_=(newPurposeId: Int)(implicit connection: Connection) = 
		putColumn(model.purposeIdColumn, newPurposeId)
	
	/**
	  * Updates the resendToken of the targeted EmailValidationAttempt instance(s)
	  * @param newResendToken A new resendToken to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def resendTokens_=(newResendToken: String)(implicit connection: Connection) = 
		putColumn(model.resendTokenColumn, newResendToken)
	
	/**
	  * Updates the token of the targeted EmailValidationAttempt instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	
	/**
	  * Updates the userId of the targeted EmailValidationAttempt instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

