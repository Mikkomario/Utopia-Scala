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
	  * Table that contains EmailValidationPurposes (An enumeration for purposes an email validation may be used for)
	  */
	def emailValidationPurpose = apply("email_validation_purpose")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = Tables(tableName)
}

