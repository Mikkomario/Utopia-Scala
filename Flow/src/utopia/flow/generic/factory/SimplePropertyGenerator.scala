package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.Property
import utopia.flow.operator.equality.EqualsBy

/**
 * This is a simple implementation of a property generator trait. The generator uses a default 
 * value and a certain property constructor.
 */
@deprecated("Please simply use PropertyFactory.apply(...) instead", "v2.0")
abstract class SimplePropertyGenerator[+T <: Property](val createProperty: (String, Value) => T,
        val defaultValue: Value = Value.empty)
    extends PropertyFactory[T] with EqualsBy
{
    // IMPLEMENTED    ------------
    
    override def apply(propertyName: String, value: Value = Value.empty) = createProperty(
            propertyName, if (value.isEmpty) defaultValue else value)
}