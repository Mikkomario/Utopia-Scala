package utopia.reach.context

import utopia.firmament.context.text.TextContextWrapper

/**
  * A common trait for context classes which wrap a Reach content window context instance
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContextWrapper[Base <: ReachContentWindowContextCopyable[Base, _], +Repr]
	extends ReachWindowContextWrapper[Base, Repr, Repr] with TextContextWrapper[Base, Repr]
		with ReachContentWindowContextCopyable[Repr, Repr]
{
	override def actorHandler = super[TextContextWrapper].actorHandler
	
	override def windowContext = base
	override def withWindowContext(base: Base): Repr = withBase(base)
}