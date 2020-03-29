package utopia.vault.database

/**
  * Thrown when database interactions fail
  * @author Mikko Hilpinen
  * @since 12.7.2019, v1.2.2+
  */
class DBException(message: String, cause: Throwable) extends RuntimeException(message, cause)
{
	/**
	  * Extends this exception, adding more information
	  * @param additionalMessage A message appended to this exception's message
	  * @return A new exception that is based on this one
	  */
	def extended(additionalMessage: String) = new DBException(message + "\n" + additionalMessage, this)
	
	/**
	  * Rethrows this exception with a new message
	  * @param message A new message
	  */
	def rethrow(message: String) = throw new DBException(message, this)
}
