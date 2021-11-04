package utopia.exodus.database

import utopia.citadel.database.Tables
import utopia.vault.model.immutable.Table

/**
  * Used for accessing the tables introduced in the Exodus project
  * @author Mikko Hilpinen
  * @since 26.6.2021, v2
  */
object ExodusTables
{
	// COMPUTED	--------------------------------
	
	/**
	  * @return A table containing registered API-keys. This table might not be used in all applications, depending
	  *         on their choice of authentication and access control.
	  */
	def apiKey = apply("api_key")
	
	/**
	  * @return Contains a list of purposes for which email validation is used
	  */
	def emailValidationPurpose = apply("email_validation_purpose")
	
	/**
	  * @return Contains email validation attempts / records
	  */
	def emailValidation = apply("email_validation")
	
	/**
	  * @return Contains email validation resend attempts / records
	  */
	def emailValidationResend = apply("email_validation_resend")
	
	/**
	  * @return Table for user authentication
	  */
	def userAuth = apply("user_authentication")
	
	/**
	  * @return Table that contains device-specific authentication keys
	  */
	def deviceAuthKey = apply("device_authentication_key")
	
	/**
	  * @return Table that contains temporary user session keys
	  */
	def userSession = apply("user_session")
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param tableName Name of targeted table
	  * @return a cached table
	  */
	private def apply(tableName: String): Table = Tables(tableName)
}
