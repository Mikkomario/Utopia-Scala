package utopia.flow.generic

import utopia.flow.collection.mutable.typeless.Variable
import utopia.flow.collection.value.typeless.Value

/**
 * SimpleVariableGenerator is just another way to create a simple property generator for variables
 */
class SimpleVariableGenerator(defaultValue: Value = Value.empty) extends
        SimplePropertyGenerator(new Variable(_, _), defaultValue)
{
    override def properties = Vector(defaultValue)    
}