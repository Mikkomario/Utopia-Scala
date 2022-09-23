package utopia.flow.parse

import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.DataType

/**
 * Value converters are able to process certain types of values into other types of information
 * @author Mikko Hilpinen
 * @since 13.12.2016
 */
trait ValueConverter[Result]
{
    /**
     * All of the data types this converter is able to convert to the desired result type
     */
    def supportedTypes: Set[DataType]
    
    /**
     * Converts the contents of the 'value' that can be assumed to be of type 'dataType' into a
     * desired result type. No empty values should be offered for write.
     * @param value The value that is written. Not empty.
     * @param dataType the data type of the provided value. The type is always
     * one of the supported data types of this converter. This can be matched against the supported 
     * types to find the correct processing method.
     * @return The processed result based on the value
     */
    def apply(value: Value, dataType: DataType): Result
}