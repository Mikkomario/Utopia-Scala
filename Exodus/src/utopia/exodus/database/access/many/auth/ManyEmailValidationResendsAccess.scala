package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationResendFactory
import utopia.exodus.database.model.auth.EmailValidationResendModel
import utopia.exodus.model.stored.auth.EmailValidationResend
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyEmailValidationResendsAccess
{
	// NESTED	--------------------
	
	private class ManyEmailValidationResendsSubView(override val parent: ManyRowModelAccess[EmailValidationResend], 
		override val filterCondition: Condition) 
		extends ManyEmailValidationResendsAccess with SubView
}

/**
  * A common trait for access points which target multiple EmailValidationResends at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
trait ManyEmailValidationResendsAccess
	extends ManyRowModelAccess[EmailValidationResend] with Indexed with FilterableView[ManyEmailValidationResendsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * validationIds of the accessible EmailValidationResends
	  */
	def validationIds(implicit connection: Connection) = 
		pullColumn(model.validationIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible EmailValidationResends
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationResendModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationResendFactory
	
	override def filter(additionalCondition: Condition): ManyEmailValidationResendsAccess = 
		new ManyEmailValidationResendsAccess.ManyEmailValidationResendsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted EmailValidationResend instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidationResend instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the validationId of the targeted EmailValidationResend instance(s)
	  * @param newValidationId A new validationId to assign
	  * @return Whether any EmailValidationResend instance was affected
	  */
	def validationIds_=(newValidationId: Int)(implicit connection: Connection) = 
		putColumn(model.validationIdColumn, newValidationId)
}

