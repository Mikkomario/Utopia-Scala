package utopia.reach.component.factory.contextual

import utopia.firmament.context.color.{StaticColorContext, StaticColorContextWrapper, VariableColorContext}

/**
  * Common trait for factories that wrap and use a ColorContext instance
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ColorContextualFactory[+Repr]
	extends ContextualFactory[StaticColorContext, Repr] with StaticColorContextWrapper[StaticColorContext, Repr]
{
	override def base: StaticColorContext = context
	override def forTextComponents: Repr = mapContext { _.forTextComponents }
	
	override def current: StaticColorContext = context
	override def toVariableContext: VariableColorContext = context.toVariableContext
	
	override def withBase(base: StaticColorContext): Repr = withContext(base)
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}
