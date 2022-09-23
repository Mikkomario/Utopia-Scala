package utopia.flow.generic

import utopia.flow.collection.mutable.typeless.Variable
import utopia.flow.collection.value.typeless.{ModelDeclaration, PropertyDeclaration, Value}
import utopia.flow.operator.Equatable

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