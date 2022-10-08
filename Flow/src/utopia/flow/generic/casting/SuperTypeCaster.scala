package utopia.flow.generic.casting

import utopia.flow.generic.model.enumeration.ConversionReliability.NoConversion
import utopia.flow.generic.model.immutable.{Conversion, Value}
import utopia.flow.generic.model.mutable.DataType

/**
 * This value caster handles "conversions" from data types to their super types. In reality, no 
 * conversion is required since the value already represents the super type.
 * @param types The types handled by this caster
 * @author Mikko Hilpinen
 * @since 13.11.2016
 */
class SuperTypeCaster(val types: Set[DataType]) extends ValueCaster
{
    // Allows conversion to any supertype
    override lazy val conversions =
        types.flatMap { dataType =>
            dataType.superType.map { superType => Conversion(dataType, superType, NoConversion) }
        }
    
    // No conversion is required since the value already represents an instance of the supertype
    override def cast(value: Value, toType: DataType) = Some(value)
}