package utopia.flow.datastructure.template

import utopia.flow.datastructure.immutable.Value
import utopia.flow.util.Equatable
import utopia.flow.generic.DataType
import utopia.flow.parse.JSONConvertible

/**
 * Properties are named and contain a value in a certain data type
 * @author Mikko Hilpinen
 * @since 26.11.2016
 */
trait Property extends JSONConvertible
{
    // PROPERTIES    -----------
    
    def value: Value
    
    /**
     * The name of the property
     */
    def name: String
    
    /**
     * The data type of this property and its contents
     */
    def dataType: DataType
    
    
    // COMP. PROPERTIES    -----
    
    override def toString = s"$name: $value"
    
    override def toJSON = "\"" + name + "\": " + value.toJSON
}