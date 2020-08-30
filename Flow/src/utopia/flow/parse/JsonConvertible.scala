package utopia.flow.parse

/**
 * Json Convertible instances can be written as json data
 * @author Mikko Hilpinen
 * @since 23.6.2017
 */
trait JsonConvertible
{
    // ABSTRACT    -----------------------
    
    /**
      * @return A json representation fo this instance
      */
    def toJson: String
    
    /**
     * A JSON Representation of this instance
     */
    @deprecated("Replaced with toJson", "v1.8")
    def toJSON = toJson
}