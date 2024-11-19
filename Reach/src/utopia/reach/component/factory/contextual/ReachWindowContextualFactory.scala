package utopia.reach.component.factory.contextual

import utopia.firmament.context.text.StaticTextContext
import utopia.reach.context.{ReachWindowContext2, ReachWindowContextWrapper2}

/**
  * Common trait for contextual factories that use a ReachWindowContext
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachWindowContextualFactory[+Repr]
	extends ContextualFactory[ReachWindowContext2, Repr]
		with ReachWindowContextWrapper2[ReachWindowContext2, Repr, Repr]
{
	override def windowContext: ReachWindowContext2 = context
	
	override def withWindowContext(base: ReachWindowContext2): Repr = withContext(base)
	override def withContentContext(textContext: StaticTextContext): Repr =
		mapContext { _.withContentContext(textContext) }
}
