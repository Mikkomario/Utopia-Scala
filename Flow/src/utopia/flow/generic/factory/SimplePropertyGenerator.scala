package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.Property

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