package utopia.reach.component.factory.contextual

import utopia.firmament.context.text.{StaticTextContext, VariableTextContext, VariableTextContextWrapper}

/**
  * Common trait for component factories utilizing variable text context.
  * @author Mikko Hilpinen
  * @since 03.07.2025, v1.6.1
  */
trait VariableTextContextualFactory[+Repr]
	extends ContextualFactory[VariableTextContext, Repr] with VariableTextContextWrapper[VariableTextContext, Repr]
{
	override def base: VariableTextContext = context
	override def current: StaticTextContext = context.current
	override def toVariableContext: VariableTextContext = context
	
	override def withBase(base: VariableTextContext): Repr = withContext(base)
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}
