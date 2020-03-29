package utopia.flow.parse

/**
 * JSON Convertible instances can be written as JSON data
 * @author Mikko Hilpinen
 * @since 23.6.2017
 */
trait JSONConvertible
{
    // ABSTRACT METHODS & PROPERTIES    ------------
    
    /**
     * A JSON Representation of this instance
     */
    def toJSON: String
}