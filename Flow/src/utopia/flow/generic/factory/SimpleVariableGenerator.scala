package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.Variable

/**
 * SimpleVariableGenerator is just another way to create a simple property generator for variables
 */
@deprecated("Please use PropertyFactory.variable instead", "v2.0")
class SimpleVariableGenerator(defaultValue: Value = Value.empty) extends
        SimplePropertyGenerator(Variable(_, _), defaultValue)
{
    protected override def equalsProperties: Iterable[Any] = Vector(defaultValue)
}