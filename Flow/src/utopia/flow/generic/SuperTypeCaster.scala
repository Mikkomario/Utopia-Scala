package utopia.flow.generic

import utopia.flow.generic.ConversionReliability.NO_CONVERSION
import utopia.flow.datastructure.immutable.Value

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
    override lazy val conversions = types.flatMap { dataType => dataType.superType.map { 
        superType => Conversion(dataType, superType, NO_CONVERSION) } }
    
    // No conversion is required since the value already represents an instance of the supertype
    override def cast(value: Value, toType: DataType) = Some(value)
}