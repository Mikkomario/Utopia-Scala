package utopia.flow.generic.operation

import utopia.flow.generic.model.enumeration.ValueOperation1
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType

/**
 * Common trait for implementations which provide unary (generic) value operations
 * @author Mikko Hilpinen
 * @since 10.11.2026, v2.9
 */
trait ValueOperator1
{
	/**
	 * @return All supported operations.
	 *         Each entry contains 2 values:
	 *         1. Supported data type
	 *         1. Supported operation
	 */
	def supportedTypes: Iterable[(DataType, ValueOperation1)]
	
	/**
	 * Applies an operation to a value.
	 * May assume that:
	 *      1. The value is non-empty
	 *      1. The value's data type + operation -combination appears in [[supportedTypes]]
	 * @param value Value to operate
	 * @param operation Operation to apply
	 * @return Operation result
	 */
	def apply(value: Value, operation: ValueOperation1): Value
}
