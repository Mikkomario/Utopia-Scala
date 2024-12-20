package utopia.reach.context

import utopia.firmament.context.window.WindowContextWrapper
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.container.{ReachCanvas, RevalidationStyle}
import utopia.reach.cursor.CursorSet

/**
  * Common trait for context classes that wrap a Reach window context instance
  * @tparam Base Wrapped window context type
  * @tparam Repr Concrete implementing type
  * @author Mikko Hilpinen
  * @since 16.4.2023, v1.0
  */
trait ReachWindowContextWrapper[Base <: ReachWindowContextCopyable[Base, _], +Repr, +Textual]
	extends WindowContextWrapper[Base, Repr] with ReachWindowContextCopyable[Repr, Textual]
{
	// IMPLEMENTED  --------------------
	
	override def windowBackground: Color = windowContext.windowBackground
	override def cursors: Option[CursorSet] = windowContext.cursors
	override def revalidationStyle: RevalidationStyle = windowContext.revalidationStyle
	override def getAnchor: (ReachCanvas, Bounds) => Point = windowContext.getAnchor
	
	override def withWindowBackground(bg: Color): Repr = mapWindowContext { _.withWindowBackground(bg) }
	override def withCursors(cursors: Option[CursorSet]): Repr = mapWindowContext { _.withCursors(cursors) }
	override def withRevalidationStyle(style: RevalidationStyle): Repr =
		mapWindowContext { _.withRevalidationStyle(style) }
	override def withGetAnchor(getAnchor: (ReachCanvas, Bounds) => Point): Repr =
		mapWindowContext { _.withGetAnchor(getAnchor) }
}
