package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.{Constant, Value}

import scala.language.implicitConversions

@deprecated("Please use PropertyFactory.constant instead", "v2.0")
object SimpleConstantGenerator
{
    val default = new SimpleConstantGenerator()
    
    implicit def objectToInstance(o: SimpleConstantGenerator.type): SimpleConstantGenerator = default
}

/**
 * A simple constant generator is another way to create a simple property generator for constants
 */
@deprecated("Please use PropertyFactory.constant instead", "v2.0")
class SimpleConstantGenerator(defaultValue: Value = Value.empty)
    extends SimplePropertyGenerator[Constant](Constant.apply, defaultValue)
{
    protected override def equalsProperties: Iterable[Any] = Vector(defaultValue)
}