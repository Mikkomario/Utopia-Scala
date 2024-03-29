package utopia.reach.context

import utopia.firmament.context.{BaseContext, TextContext, TextContextWrapper}

/**
  * Common trait for contexts that are used for creating pop-up windows.
  * Combines TextContext and ReachWindowContext information.
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContextLike[+Repr] extends TextContextWrapper[Repr] with ReachWindowContextWrapper[Repr, Repr]
{
	override def actorHandler = super[ReachWindowContextWrapper].actorHandler
	override def withContentContext(context: BaseContext) = withBase(context)
	override def withContentContext(textContext: TextContext): Repr = withTextContext(textContext)
}