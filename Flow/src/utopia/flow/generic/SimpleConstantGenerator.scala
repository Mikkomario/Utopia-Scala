package utopia.flow.generic

import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable.Constant

import scala.language.implicitConversions

object SimpleConstantGenerator
{
    val default = new SimpleConstantGenerator()
    
    implicit def objectToInstance(o: SimpleConstantGenerator.type): SimpleConstantGenerator = default
}

/**
 * A simple constant generator is another way to create a simple property generator for constants
 */
class SimpleConstantGenerator(defaultValue: Value = Value.empty)
    extends SimplePropertyGenerator[Constant](Constant.apply, defaultValue)
{
    override def properties = Vector(defaultValue)
}