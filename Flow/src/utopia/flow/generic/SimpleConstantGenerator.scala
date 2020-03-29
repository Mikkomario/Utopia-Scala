package utopia.flow.generic

import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable.Constant

/**
 * A simple constant generator is another way to create a simple property generator for constants
 */
class SimpleConstantGenerator(defaultValue: Value = Value.empty) extends SimplePropertyGenerator[Constant](Constant.apply, defaultValue)
{
    override def properties = Vector(defaultValue)
}