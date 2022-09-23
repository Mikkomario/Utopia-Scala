package utopia.flow.generic

import utopia.flow.collection.value.typeless.Value

/**
 * Instances implementing this trait are able to perform certain data type conversions, casting 
 * values to different data types
 * @author Mikko Hilpinen
 * @since 12.11.2016
 */
trait ValueCaster
{
    /**
     * These are all possible conversions that can be made using this caster instance
     */
    val conversions: Set[Conversion]
    
     /**
     * Casts a value to a different data type entirely. This method should never be called when 
     * the value is already of the desired type. Also, this method should only be called to perform 
     * conversions marked possible by this caster instance.
     * @param value The source value that is being casted
     * @param toType The target data type of the cast
     * @return The casted value or None if the casting failed
     */
    def cast(value: Value, toType: DataType): Option[Value]
}