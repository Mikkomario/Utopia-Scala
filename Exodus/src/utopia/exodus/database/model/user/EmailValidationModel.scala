package utopia.exodus.database.model.user

import java.time.Instant

import utopia.exodus.database.factory.user.EmailValidationFactory
import utopia.exodus.model.partial.EmailValidationData
import utopia.exodus.model.stored.EmailValidation
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object EmailValidationModel
{
	// OTHER	----------------------------
	
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
		val id = apply(None, Some(data.purposeId), Some(data.email), Some(data.key), Some(data.resendKey),
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
								created: Option[Instant] = None, expiration: Option[Instant] = None,
								actualization: Option[Instant] = None)
	extends StorableWithFactory[EmailValidation]
{
	override def factory = EmailValidationFactory
	
	override def valueProperties = Vector("id" -> id, "purposeId" -> purposeId, "email" -> email, "key" -> key,
		"resendKey" -> resendKey, "created" -> created, "expiresIn" -> expiration, "actualizedIn" -> actualization)
}
