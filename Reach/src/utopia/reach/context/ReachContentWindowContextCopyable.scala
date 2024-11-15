package utopia.reach.context

import utopia.firmament.context.text.TextContextCopyable

/**
  * Common trait for Reach window context implementations that may be copied and which also specify settings for
  * textual content
  * @author Mikko Hilpinen
  * @since 14.11.2024, v1.5
  */
trait ReachContentWindowContextCopyable[+Repr]
	extends ReachWindowContextCopyable[Repr, Repr] with TextContextCopyable[Repr] with ReachContentWindowPropsView
{
	override def current: StaticReachContentWindowContext
	override def toVariableContext: VariableReachContentWindowContext
}