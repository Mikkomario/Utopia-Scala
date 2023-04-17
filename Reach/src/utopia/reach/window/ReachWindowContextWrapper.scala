package utopia.reach.window
import utopia.firmament.context.WindowContext
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.container.{ReachCanvas2, RevalidationStyle}
import utopia.reach.cursor.CursorSet
import utopia.reflection.component.drawing.template.CustomDrawer

/**
  * Common trait for context classes that wrap a Reach window context instance
  * @author Mikko Hilpinen
  * @since 16.4.2023, v1.0
  */
trait ReachWindowContextWrapper[+Repr] extends ReachWindowContextLike[Repr]
{
	// ABSTRACT -------------------------
	
	def wrapped: ReachWindowContext
	
	/**
	  * @param base A new Reach window context to wrap
	  * @return A copy of this item with that wrapped context
	  */
	def withReachBase(base: ReachWindowContext): Repr
	
	
	// IMPLEMENTED  --------------------
	
	override def cursors: Option[CursorSet] = wrapped.cursors
	override def revalidationStyle: RevalidationStyle = wrapped.revalidationStyle
	override def customDrawers: Vector[CustomDrawer] = wrapped.customDrawers
	override def getAnchor: (ReachCanvas2, Bounds) => Point = wrapped.getAnchor
	
	override def withCursors(cursors: Option[CursorSet]): Repr = mapReachBase { _.withCursors(cursors) }
	override def withRevalidationStyle(style: RevalidationStyle): Repr = mapReachBase { _.withRevalidationStyle(style) }
	override def withCustomDrawers(customDrawers: Vector[CustomDrawer]): Repr =
		mapReachBase { _.withCustomDrawers(customDrawers) }
	override def withGetAnchor(getAnchor: (ReachCanvas2, Bounds) => Point): Repr =
		mapReachBase { _.withGetAnchor(getAnchor) }
	
	override def withBase(base: WindowContext): Repr =
		if (base == wrapped.wrapped) self else mapReachBase { _.withBase(base) }
	
	
	// OTHER    ------------------------
	
	private def mapReachBase(f: ReachWindowContext => ReachWindowContext) = withReachBase(f(wrapped))
}
