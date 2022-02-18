package utopia.exodus.database

import utopia.citadel.database.Tables
import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object ExodusTables
{
	// COMPUTED	--------------------
	
	/**
	  * Table that contains email validation attempts (Represents an attempted email validation. Provides additional
	  *  information to an authentication token.)
	  */
	def emailValidationAttempt = apply("email_validation_attempt")
	
	/**
	  * Table that contains scopes (Represents an access right requirement and/or category.)
	  */
	def scope = apply("scope")
	
	/**
	  * Table that contains tokens (Tokens used for authenticating requests)
	  */
	def token = apply("token")
	
	/**
	  * Table that contains token scope links (Used for linking scopes to tokens using many-to-many connections, 
		
	  * describing what actions each token enables)
	  */
	def tokenScopeLink = apply("token_scope_link")
	
	/**
	  * 
		Table that contains token types (An enumeration for different types of authentication tokens available)
	  */
	def tokenType = apply("token_type")
	
	/**
	  * Table that contains user passwords (Represents a hashed user password)
	  */
	def userPassword = apply("user_password")
	
	/**
	  * Table that contains ApiKeys (Used for authenticating requests before session-based authentication is available)
	  */
	@deprecated("Will be removed in a future release", "v4.0")
	def apiKey = apply("api_key")
	
	/**
	  * Table that contains DeviceTokens (Used as a refresh token to generate device-specific session tokens on private devices)
	  */
	@deprecated("Will be removed in a future release", "v4.0")
	def deviceToken = apply("device_token")
	
	/**
	  * Table that contains EmailValidationPurposes (An enumeration for purposes an email validation may be used for)
	  */
	@deprecated("Will be removed in a future release", "v4.0")
	def emailValidationPurpose = apply("email_validation_purpose")
	
	/**
	  * Table that contains EmailValidationResends (Represents a time when an email validation was sent again)
	  */
	@deprecated("Will be removed in a future release", "v4.0")
	def emailValidationResend = apply("email_validation_resend")
	
	/**
	  * Table that contains EmailValidatedSessions (Used for creating a temporary and limited session based on an
	  *  authenticated email validation attempt)
	  */
	@deprecated("Will be removed in a future release", "v4.0")
	def emailValidatedSession = apply("email_validated_session")
	
	/**
	  * Table that contains SessionTokens (Used for authenticating temporary user sessions)
	  */
	@deprecated("Will be removed in a future release", "v4.0")
	def sessionToken = apply("session_token")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = Tables(tableName)
}

