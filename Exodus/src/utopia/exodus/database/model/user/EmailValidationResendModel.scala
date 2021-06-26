package utopia.exodus.database.model.user

import utopia.exodus.database.{ExodusTables, Tables}
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable

object EmailValidationResendModel
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return The table used by this model
	  */
	def table = ExodusTables.emailValidationResend
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param validationId Id of the targeted email validation
	  * @return A model with only validation id set
	  */
	def withValidationId(validationId: Int) = apply(validationId = Some(validationId))
	
	/**
	  * Inserts a new validation resend record to the database
	  * @param validationId Id of the targeted email validation attempt
	  * @param connection DB Connection (implicit)
	  * @return Id of the newly recorded resend record
	  */
	def insert(validationId: Int)(implicit connection: Connection) =
		apply(None, Some(validationId)).insert().getInt
}

/**
  * Used for interacting with email validation resend data in DB
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
case class EmailValidationResendModel(id: Option[Int] = None, validationId: Option[Int] = None) extends Storable
{
	override def table = EmailValidationResendModel.table
	
	override def valueProperties = Vector("id" -> id, "validationId" -> validationId)
}
