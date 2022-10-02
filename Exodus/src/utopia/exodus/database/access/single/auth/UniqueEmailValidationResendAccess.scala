package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationResendFactory
import utopia.exodus.database.model.auth.EmailValidationResendModel
import utopia.exodus.model.stored.auth.EmailValidationResend
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct EmailValidationResends.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
trait UniqueEmailValidationResendAccess 
	extends SingleRowModelAccess[EmailValidationResend] 
		with DistinctModelAccess[EmailValidationResend, Option[EmailValidationResend], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the email_validation_attempt linked 
		with this EmailValidationResend. None if no instance (or value) was found.
	  */
	def validationId(implicit connection: Connection) = pullColumn(model.validationIdColumn).int
	
	/**
	  * Time when this EmailValidationResend was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationResendModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationResendFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted EmailValidationResend instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidationResend instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the validationId of the targeted EmailValidationResend instance(s)
	  * @param newValidationId A new validationId to assign
	  * @return Whether any EmailValidationResend instance was affected
	  */
	def validationId_=(newValidationId: Int)(implicit connection: Connection) = 
		putColumn(model.validationIdColumn, newValidationId)
}

