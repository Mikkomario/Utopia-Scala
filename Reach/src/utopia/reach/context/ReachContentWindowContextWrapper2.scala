package utopia.reach.context

import utopia.firmament.context.text.TextContextWrapper2

/**
  * A common trait for context classes which wrap a Reach content window context instance
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContextWrapper2[Base <: ReachContentWindowContextCopyable[Base], +Repr]
	extends ReachWindowContextWrapper2[Base, Repr, Repr] with TextContextWrapper2[Base, Repr]
		with ReachContentWindowContextCopyable[Repr]
{
	override def windowContext = base
	override def withWindowContext(base: Base): Repr = withBase(base)
}