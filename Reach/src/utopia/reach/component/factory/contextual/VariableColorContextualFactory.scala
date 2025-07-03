package utopia.reach.component.factory.contextual

import utopia.firmament.context.color.{StaticColorContext, VariableColorContext, VariableColorContextWrapper}

/**
  * Common trait for component factories that utilize variable color context.
  * @author Mikko Hilpinen
  * @since 03.07.2025, v1.6.1
  */
trait VariableColorContextualFactory[+Repr]
	extends ContextualFactory[VariableColorContext, Repr] with VariableColorContextWrapper[VariableColorContext, Repr]
{
	override def base: VariableColorContext = context
	override def forTextComponents: Repr = mapContext { _.forTextComponents }
	override def current: StaticColorContext = context.current
	override def toVariableContext: VariableColorContext = context
	
	override def withBase(base: VariableColorContext): Repr = withContext(context)
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}
