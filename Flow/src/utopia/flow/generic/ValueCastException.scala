package utopia.flow.generic

import utopia.flow.collection.value.typeless.Value

/**
 * These exceptions are thrown when value casting fails
 * @author Mikko Hilpinen
 * @since 12.11.2016
 * @deprecated Value casting will return None on failure in the future (or present)
 */
@deprecated("Optionals are now used instead of exceptions.", "v.1.0.1")
class ValueCastException(val sourceValue: Value, val targetType: DataType, cause: Throwable = null) 
        extends DataTypeException(s"Failed to cast $sourceValue (${sourceValue.dataType
        }) to $targetType", cause)