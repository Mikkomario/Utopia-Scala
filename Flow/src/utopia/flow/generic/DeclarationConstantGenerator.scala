package utopia.flow.generic

import utopia.flow.collection.value.typeless.{Constant, ModelDeclaration, Value}

class DeclarationConstantGenerator(declaration: ModelDeclaration,
        defaultValue: Value = Value.empty) extends
        DeclarationPropertyGenerator(Constant.apply, declaration, defaultValue)
{
    override def properties = Vector(declaration, defaultValue)    
}