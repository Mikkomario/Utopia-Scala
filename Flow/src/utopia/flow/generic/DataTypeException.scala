package utopia.flow.generic

/**
 * These exceptions are thrown when data types are misused. They are often considered fatal
 * programming errors.
 * @author Mikko Hilpinen
 * @since 12.11.2016
 * @param message The message sent along with the exception
 * @param cause The cause of the exception (optional)
 */
case class DataTypeException(val message: String, val cause: Throwable = null) extends 
        RuntimeException(message, cause)
{
    /**
     * Creates a new exception
     * @param dataType The type that was used incorrectly and caused the exception
     */
    def this(dataType: DataType) = this(s"Invalid use of $dataType")
}