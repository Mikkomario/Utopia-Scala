package utopia.flow.parse

import scala.collection.immutable.HashMap
import utopia.flow.generic.DataType
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ConversionHandler

/**
 * An instance of this class is able to process values using possibly multiple different converters.
 * The instance is mutable since converters may be introduced as necessary.
 * @author Mikko Hilpinen
 * @since 25.4.2017
 */
class ValueConverterManager[Result](initialConverters: Iterable[ValueConverter[Result]])
{
    // PROPERTIES    -----------
    
    private var converters = HashMap[DataType, ValueConverter[Result]]()
    
    
    // INITIAL CODE    ---------
    
    // Adds the initial converters
    initialConverters.foreach { introduce }
    
    
    // OTHER METHODS    --------
    
    /**
     * Introduces a new converter implementation to this interface. If the converter provides
     * implementation for some already covered data type, the new implementation will override the
     * previous one.
     * @param converter The converter that is added to this manager interface
     */
    def introduce(converter: ValueConverter[Result]) = converter.supportedTypes.foreach {
            converters += Tuple2(_, converter) }
    
    /**
     * Processes a value using the introduced converters
     * @param value The value that will be converted
     * @return The converted value or None if the value was empty or no suitable converter could be
     * found for the value
     */
    def apply(value: Value) = 
    {
        // Casts the value to a compatible type
        val casted = ConversionHandler.cast(value, converters.keySet)
        
        // Only non-empty values are converted
        if (casted.exists { _.isDefined })
        {
            // Searches for a direct converter first, then an indirect converter
            converters.get(casted.get.dataType) match
            {
                case Some(directConverter) => Some(directConverter(casted.get, casted.get.dataType))
                case None =>
                    converters.keys.find { casted.get.dataType isOfType _ }
                        .map { wrappedType => converters(wrappedType)(casted.get, wrappedType) }
            }
        }
        else
            None
    }
}