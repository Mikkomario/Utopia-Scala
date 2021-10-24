package utopia.exodus.database.model.user

import utopia.vault.nosql.storable.deprecation.Expiring

import java.time.Instant
import utopia.exodus.database.factory.user.EmailValidationFactory
import utopia.exodus.model.partial.EmailValidationData
import utopia.exodus.model.stored.EmailValidation
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object EmailValidationModel extends Expiring
{
	// ATTRIBUTES   ------------------------
	
	override val deprecationAttName = "expiresIn"
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return The factory class used by this model
	  */
	def factory = EmailValidationFactory
	
	
	// IMPLEMENTED  ------------------------
	
	override def table = factory.table
	
	
	// OTHER	----------------------------
	
	/**
	  * @param validationId Id of the targeted validation
	  * @return A model with only id set
	  */
	def withId(validationId: Int) = apply(Some(validationId))
	
	/**
	  * @param email An email address
	  * @return A model with specified email address
	  */
	def withEmail(email: String) = apply(email = Some(email))
	
	/**
	  * @param key An email validation key
	  * @return A model with only validation key set
	  */
	def withKey(key: String) = apply(key = Some(key))
	
	/**
	  * @param key An email resend key
	  * @return A model with only resend key set
	  */
	def withResendKey(key: String) = apply(resendKey = Some(key))
	
	/**
	  * @param expiration A validation expiration time
	  * @return A model with only expiration time set
	  */
	def withExpiration(expiration: Instant) = apply(expiration = Some(expiration))
	
	/**
	  * Inserts a new email validation attempt to the DB
	  * @param data Email validation data
	  * @param connection DB Connection (implicit)
	  * @return The newly inserted validation attempt
	  */
	def insert(data: EmailValidationData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(data.purposeId), Some(data.email), Some(data.key), Some(data.resendKey), data.ownerId,
			Some(data.created), Some(data.expiration), data.actualized).insert().getInt
		EmailValidation(id, data)
	}
}

/**
  * Used for interacting with email validation recordings in DB
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
case class EmailValidationModel(id: Option[Int] = None, purposeId: Option[Int] = None, email: Option[String] = None,
								key: Option[String] = None, resendKey: Option[String] = None,
								ownerId: Option[Int] = None, created: Option[Instant] = None,
								expiration: Option[Instant] = None, actualization: Option[Instant] = None)
	extends StorableWithFactory[EmailValidation]
{
	import EmailValidationModel._
	
	// COMPUTED	------------------------------
	
	/**
	  * @return A copy of this model that has just been marked as actualized / answered
	  */
	def nowActualized = copy(actualization = Some(Now))
	
	
	// IMPLEMENTED	--------------------------
	
	override def factory = EmailValidationModel.factory
	
	override def valueProperties = Vector("id" -> id, "purposeId" -> purposeId, "email" -> email, "key" -> key,
		"resendKey" -> resendKey, "ownerId" -> ownerId, "created" -> created, deprecationAttName -> expiration,
		"actualizedIn" -> actualization)
	
	
	// OTHER	------------------------------
	
	/**
	  * @param purposeId Id of the purpose of this validation
	  * @return A model with purpose id set
	  */
	def withPurposeId(purposeId: Int) = copy(purposeId = Some(purposeId))
}
