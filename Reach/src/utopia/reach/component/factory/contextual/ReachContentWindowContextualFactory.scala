package utopia.reach.component.factory.contextual

import utopia.reach.context.{ReachContentWindowContext, ReachContentWindowContextWrapper}

/**
  * Common trait for factories which wrap and use a popup creation context
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContextualFactory[+Repr]
	extends ContextualFactory[ReachContentWindowContext, Repr] with ReachContentWindowContextWrapper[Repr]
{
	override def contentWindowContext: ReachContentWindowContext = context
	override def withContentWindowContext(base: ReachContentWindowContext): Repr = withContext(base)
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}
