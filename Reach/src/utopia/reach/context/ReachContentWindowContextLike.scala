package utopia.reach.context

import utopia.firmament.context.base.StaticBaseContext
import utopia.firmament.context.text.{StaticTextContext, StaticTextContextWrapper}

/**
  * Common trait for contexts that are used for creating pop-up windows.
  * Combines TextContext and ReachWindowContext information.
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContextLike[+Repr]
	extends StaticTextContextWrapper[StaticTextContext, Repr] with ReachWindowContextWrapper[Repr, Repr]
{
	override def actorHandler = super[ReachWindowContextWrapper].actorHandler
	
	override def withContentContext(context: StaticBaseContext) =
		withBase(context.against(background).forTextComponents)
	override def withContentContext(textContext: StaticTextContext): Repr = withBase(textContext)
}