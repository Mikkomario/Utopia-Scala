package utopia.reach.context

import utopia.firmament.context.TextContextWrapper
import utopia.paradigm.color.Color

/**
  * Common trait for contexts that are used for creating pop-up windows.
  * Combines TextContext and ReachWindowContext information.
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait PopupContextLike[+Repr] extends TextContextWrapper[Repr] with ReachWindowContextWrapper[Repr, Repr]
{
	override def actorHandler = super[ReachWindowContextWrapper].actorHandler
	override def background = super[TextContextWrapper].background
	override def withBackground(bg: Color) = super[TextContextWrapper].withBackground(bg)
	override def mapBackground(f: Color => Color) = super[TextContextWrapper].mapBackground(f)
}