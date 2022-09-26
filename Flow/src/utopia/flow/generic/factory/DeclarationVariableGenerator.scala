package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.{ModelDeclaration, Value}
import utopia.flow.generic.model.mutable.Variable

/**
 * This variable generator uses property declarations when generating new variables
 * @author Mikko Hilpinen
 * @since 11.12.2016
 */
@deprecated("Please use ModelDeclaration.toVariableFactory instead", "v2.0")
class DeclarationVariableGenerator(declaration: ModelDeclaration, defaultValue: Value = Value.empty)
        extends DeclarationPropertyGenerator(new Variable(_, _), declaration, defaultValue)
{
    protected override def equalsProperties: Iterable[Any] = Vector(declaration, defaultValue)
}