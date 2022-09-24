package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.{ModelDeclaration, Value}
import utopia.flow.generic.model.mutable.Variable

/**
 * This variable generator uses property declarations when generating new variables
 * @author Mikko Hilpinen
 * @since 11.12.2016
 */
class DeclarationVariableGenerator(declaration: ModelDeclaration, defaultValue: Value = Value.empty)
        extends DeclarationPropertyGenerator(new Variable(_, _), declaration, defaultValue)
{
    override def properties = Vector(declaration, defaultValue)    
}