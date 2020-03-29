package utopia.vault.sql

/**
 * These exceptions are thrown when sql statements are created improperly and would not work
 * @author Mikko Hilpinen
 * @since 12.10.2019, v1.4+
 */
class MalformedSQLException(message: String, cause: Option[Throwable] = None) extends RuntimeException(message, cause.orNull)
