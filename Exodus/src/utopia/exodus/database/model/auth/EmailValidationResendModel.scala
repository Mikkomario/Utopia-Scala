package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationResendFactory
import utopia.exodus.model.partial.auth.EmailValidationResendData
import utopia.exodus.model.stored.auth.EmailValidationResend
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing EmailValidationResendModel instances and for inserting EmailValidationResends to the database
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object EmailValidationResendModel 
	extends DataInserter[EmailValidationResendModel, EmailValidationResend, EmailValidationResendData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains EmailValidationResend validationId
	  */
	val validationIdAttName = "validationId"
	
	/**
	  * Name of the property that contains EmailValidationResend created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains EmailValidationResend validationId
	  */
	def validationIdColumn = table(validationIdAttName)
	
	/**
	  * Column that contains EmailValidationResend created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = EmailValidationResendFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: EmailValidationResendData) = apply(None, Some(data.validationId), 
		Some(data.created))
	
	override def complete(id: Value, data: EmailValidationResendData) = EmailValidationResend(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this EmailValidationResend was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A EmailValidationResend id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param validationId Id of the email_validation_attempt linked with this EmailValidationResend
	  * @return A model containing only the specified validationId
	  */
	def withValidationId(validationId: Int) = apply(validationId = Some(validationId))
}

/**
  * Used for interacting with EmailValidationResends in the database
  * @param id EmailValidationResend database id
  * @param validationId Id of the email_validation_attempt linked with this EmailValidationResend
  * @param created Time when this EmailValidationResend was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class EmailValidationResendModel(id: Option[Int] = None, validationId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[EmailValidationResend]
{
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationResendModel.factory
	
	override def valueProperties = 
	{
		import EmailValidationResendModel._
		Vector("id" -> id, validationIdAttName -> validationId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param validationId A new validationId
	  * @return A new copy of this model with the specified validationId
	  */
	def withValidationId(validationId: Int) = copy(validationId = Some(validationId))
}

