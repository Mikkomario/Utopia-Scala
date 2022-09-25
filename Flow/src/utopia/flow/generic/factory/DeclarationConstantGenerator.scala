package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.{Constant, ModelDeclaration, Value}

class DeclarationConstantGenerator(declaration: ModelDeclaration,
        defaultValue: Value = Value.empty) extends
        DeclarationPropertyGenerator(Constant.apply, declaration, defaultValue)
{
    protected override def equalsProperties: Iterable[Any] = Vector(declaration, defaultValue)
}