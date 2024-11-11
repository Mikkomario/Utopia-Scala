package utopia.reach.component.factory.contextual

import utopia.firmament.context.text.StaticTextContext
import utopia.reach.context.{ReachWindowContext, ReachWindowContextWrapper}

/**
  * Common trait for contextual factories that use a ReachWindowContext
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachWindowContextualFactory[+Repr]
	extends ContextualFactory[ReachWindowContext, Repr] with ReachWindowContextWrapper[Repr, Repr]
{
	override def reachWindowContext: ReachWindowContext = context
	
	override def withReachWindowContext(base: ReachWindowContext): Repr = withContext(base)
	override def withContentContext(textContext: StaticTextContext): Repr =
		mapContext { _.withContentContext(textContext) }
}
