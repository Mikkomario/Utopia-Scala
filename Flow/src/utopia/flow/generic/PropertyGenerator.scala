package utopia.flow.generic

import utopia.flow.datastructure.template.Property
import utopia.flow.datastructure.immutable.Value
import utopia.flow.operator.Equatable

/**
 * Property generators are used for generating properties of different types
 */
trait PropertyGenerator[+T <: Property] extends Equatable
{
    /**
     * Generates a new property
     * @param propertyName The name for the new property
     * @param value The value for the new property (optional)
     * @return Generated property
     */
    def apply(propertyName: String, value: Option[Value] = None): T
}