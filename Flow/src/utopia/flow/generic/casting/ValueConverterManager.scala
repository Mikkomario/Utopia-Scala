package utopia.flow.generic.casting

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType

/**
 * An instance of this class is able to process values using possibly multiple different converters.
 * The instance is mutable since converters may be introduced as necessary.
 * @author Mikko Hilpinen
 * @since 25.4.2017
 */
class ValueConverterManager[Result](initialConverters: Iterable[ValueConverter[Result]])
{
    // PROPERTIES    -----------
    
    private var converters = Map[DataType, ValueConverter[Result]]()
    
    
    // INITIAL CODE    ---------
    
    // Adds the initial converters
    initialConverters.foreach(introduce)
    
    
    // OTHER METHODS    --------
    
    /**
     * Introduces a new converter implementation to this interface. If the converter provides
     * implementation for some already covered data type, the new implementation will override the
     * previous one.
     * @param converter The converter that is added to this manager interface
     */
    def introduce(converter: ValueConverter[Result]) =
        converters ++= converter.supportedTypes.map { _ -> converter }
    
    /**
     * Processes a value using the introduced converters
     * @param value The value that will be converted
     * @return The converted value or None if the value was empty or no suitable converter could be
     * found for the value
     */
    def apply(value: Value) = {
        // Casts the value to a compatible type
        // Only non-empty values are converted
        ConversionHandler.cast(value, converters.keySet).filter { _.isDefined }.flatMap { casted =>
            // Searches for a direct converter first, then an indirect converter
            converters.get(casted.dataType) match {
                case Some(directConverter) => Some(directConverter(casted, casted.dataType))
                case None =>
                    converters.keys.find(casted.dataType.isOfType)
                        .map { wrappedType => converters(wrappedType)(casted, wrappedType) }
            }
        }
    }
}