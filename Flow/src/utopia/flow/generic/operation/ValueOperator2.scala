package utopia.flow.generic.operation

import utopia.flow.generic.model.enumeration.ValueOperation2
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType

/**
 * Applies operations that affect two values at a time
 * @author Mikko Hilpinen
 * @since 10.11.2026, v2.9
 */
trait ValueOperator2
{
	/**
	 * @return All operations that are supported by this operator.
	 *         Each entry contains 3 values:
	 *         1. Supported primary data type
	 *         1. Supported operation
	 *         1. Supported secondary data types
	 */
	def supportedOperations: Iterable[(DataType, ValueOperation2, Set[DataType])]
	
	/**
	 * Applies a (supported) value operation.
	 * May assume that:
	 *      - Both primary and secondary are non-empty
	 *      - Combination of primary + operation + secondary appears in [[supportedOperations]]
	 * @param primary Primary value
	 * @param operation Operation to apply
	 * @param secondary Secondary value
	 * @return Operation result
	 */
	def apply(primary: Value, operation: ValueOperation2, secondary: Value): Value
}
