package utopia.flow.datastructure.template

/**
 * These exceptions are thrown when a certain attribute can't be found from a model
 * @author Mikko Hilpinen
 * @since 26.11.2016
 */
@deprecated("Please use NoSuchElementException instead", "v2.0")
class NoSuchAttributeException(val message: String, val cause: Throwable = null) extends 
        RuntimeException(message, cause)