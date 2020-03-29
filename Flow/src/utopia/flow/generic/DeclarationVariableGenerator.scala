package utopia.flow.generic

import utopia.flow.datastructure.immutable.PropertyDeclaration
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.datastructure.mutable.Variable
import utopia.flow.util.Equatable

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