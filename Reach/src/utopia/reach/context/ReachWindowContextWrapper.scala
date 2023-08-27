package utopia.reach.context

import utopia.firmament.context.WindowContext
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.container.{ReachCanvas, RevalidationStyle}
import utopia.reach.cursor.CursorSet

/**
  * Common trait for context classes that wrap a Reach window context instance
  * @author Mikko Hilpinen
  * @since 16.4.2023, v1.0
  */
trait ReachWindowContextWrapper[+Repr, +Textual] extends ReachWindowContextLike[Repr, Textual]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The wrapped Reach window context instance
	  */
	def reachWindowContext: ReachWindowContext
	
	/**
	  * @param base A new Reach window context to wrap
	  * @return A copy of this item with that wrapped context
	  */
	def withReachWindowContext(base: ReachWindowContext): Repr
	
	
	// IMPLEMENTED  --------------------
	
	override def windowContext: WindowContext = reachWindowContext
	
	override def windowBackground: Color = reachWindowContext.windowBackground
	override def cursors: Option[CursorSet] = reachWindowContext.cursors
	override def revalidationStyle: RevalidationStyle = reachWindowContext.revalidationStyle
	override def getAnchor: (ReachCanvas, Bounds) => Point = reachWindowContext.getAnchor
	
	override def withWindowBackground(bg: Color): Repr = mapReachWindowContext { _.withWindowBackground(bg) }
	override def withCursors(cursors: Option[CursorSet]): Repr = mapReachWindowContext { _.withCursors(cursors) }
	override def withRevalidationStyle(style: RevalidationStyle): Repr = mapReachWindowContext { _.withRevalidationStyle(style) }
	override def withGetAnchor(getAnchor: (ReachCanvas, Bounds) => Point): Repr =
		mapReachWindowContext { _.withGetAnchor(getAnchor) }
	
	override def withWindowContext(base: WindowContext): Repr =
		if (base == windowContext) self else mapReachWindowContext { _.withWindowContext(base) }
	
	
	// OTHER    ------------------------
	
	/**
	  * @param f A mapping function for the wrapped ReachWindowContext
	  * @return A copy of this context with mapped Reach window context
	  */
	def mapReachWindowContext(f: ReachWindowContext => ReachWindowContext) =
		withReachWindowContext(f(reachWindowContext))
}
