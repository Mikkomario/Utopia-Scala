package utopia.reach.component.factory.contextual

import utopia.firmament.context.text.StaticTextContext
import utopia.reach.context.{ReachContentWindowContextWrapper2, StaticReachContentWindowContext}

/**
  * Common trait for factories which wrap and use a popup creation context
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContextualFactory[+Repr]
	extends ContextualFactory[StaticReachContentWindowContext, Repr]
		with ReachContentWindowContextWrapper2[StaticReachContentWindowContext, Repr]
{
	override def base: StaticReachContentWindowContext = context
	override def current = windowContext
	override def toVariableContext = windowContext.toVariableContext
	
	override def withBase(base: StaticReachContentWindowContext): Repr = withContext(base)
	override def withContentContext(textContext: StaticTextContext): Repr = mapBase { _.withContentContext(textContext) }
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}
