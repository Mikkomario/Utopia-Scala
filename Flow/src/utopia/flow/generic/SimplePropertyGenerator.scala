package utopia.flow.generic

import utopia.flow.datastructure.template.Property
import utopia.flow.datastructure.immutable.Value
import utopia.flow.operator.Equatable

/**
 * This is a simple implementation of a property generator trait. The generator uses a default 
 * value and a certain property constructor.
 */
abstract class SimplePropertyGenerator[+T <: Property](val createProperty: (String, Value) => T,
        val defaultValue: Value = Value.empty) extends PropertyGenerator[T]
{
    // IMPLEMENTED    ------------
    
    override def apply(propertyName: String, value: Option[Value] = None) = createProperty(
            propertyName, value.getOrElse(defaultValue))
}