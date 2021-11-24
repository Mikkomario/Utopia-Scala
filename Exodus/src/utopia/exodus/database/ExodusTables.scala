package utopia.exodus.database

import utopia.citadel.database.Tables
import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object ExodusTables
{
	// COMPUTED	--------------------
	
	/**
	  * Table that contains ApiKeys (Used for authenticating requests before session-based authentication is available)
	  */
	def apiKey = apply("api_key")
	
	/**
	  * Table that contains DeviceTokens (Used as a refresh token to generate device-specific session tokens on private devices)
	  */
	def deviceToken = apply("device_token")
	
	/**
	  * Table that contains EmailValidationAttempts (Represents an attempted email validation, 
		and the possible response / success)
	  */
	def emailValidationAttempt = apply("email_validation_attempt")
	
	/**
	  * Table that contains EmailValidationPurposes (An enumeration for purposes an email validation may be used for)
	  */
	def emailValidationPurpose = apply("email_validation_purpose")
	
	/**
	  * Table that contains EmailValidationResends (Represents a time when an email validation was sent again)
	  */
	def emailValidationResend = apply("email_validation_resend")
	
	/**
	  * Table that contains EmailValidatedSessions (Used for creating a temporary and limited session based on an
	  *  authenticated email validation attempt)
	  */
	def emailValidatedSession = apply("email_validated_session")
	
	/**
	  * Table that contains SessionTokens (Used for authenticating temporary user sessions)
	  */
	def sessionToken = apply("session_token")
	
	/**
	  * Table that contains UserPasswords (Represents a hashed user password)
	  */
	def userPassword = apply("user_password")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = Tables(tableName)
}

