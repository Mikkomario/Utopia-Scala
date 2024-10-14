package utopia.reach.component.factory.contextual

import utopia.firmament.context.text.{StaticTextContext, StaticTextContextWrapper, VariableTextContext}

/**
  * Common trait for factories that use a TextContext (only)
  * @author Mikko Hilpinen
  * @since 17.4.2023, v0.1
  */
trait TextContextualFactory[+Repr]
	extends ContextualFactory[StaticTextContext, Repr] with StaticTextContextWrapper[StaticTextContext, Repr]
{
	override def base: StaticTextContext = context
	override def withBase(base: StaticTextContext): Repr = withContext(base)
	
	override def current: StaticTextContext = context
	override def toVariableContext: VariableTextContext = context.toVariableContext
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}
