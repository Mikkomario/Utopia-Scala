package utopia.vault.model.error

/**
  * Thrown when database columns can't be found
  * @author Mikko Hilpinen
  * @since 18.4.2021, v1.7.1
  */
class ColumnNotFoundException(message: String, cause: Exception = null) extends RuntimeException(message, cause)
