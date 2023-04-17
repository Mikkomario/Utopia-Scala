package utopia.reach.window

import utopia.firmament.context.WindowContext
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.container.RevalidationStyle.Immediate
import utopia.reach.container.{ReachCanvas2, RevalidationStyle}
import utopia.reach.cursor.CursorSet
import utopia.reflection.component.drawing.template.CustomDrawer

import scala.language.implicitConversions

object ReachWindowContext
{
	// IMPLICIT -----------------------
	
	implicit def wrap(c: WindowContext): ReachWindowContext = apply(c)
	
	
	// OTHER    -----------------------
	
	/**
	  * Creates a new reach window context
	  * @param base Window creation context to wrap
	  * @param cursors Set of cursors to use (optional)
	  * @param revalidationStyle Revalidation style to use (default = revalidate immediately in a separate thread)
	  * @param customDrawers Custom drawers to assign to windows (default = empty)
	  * @param getAnchor Anchoring function (default = anchor over the focused component, or at the window center)
	  * @return A new reach window context
	  */
	def apply(base: WindowContext, cursors: Option[CursorSet] = None,
	          revalidationStyle: RevalidationStyle = Immediate.async, customDrawers: Vector[CustomDrawer] = Vector(),
	          getAnchor: (ReachCanvas2, Bounds) => Point = _.anchorPosition(_)): ReachWindowContext =
		_ReachWindowContext(base, cursors, revalidationStyle, customDrawers, getAnchor)
	
	
	// NESTED   -----------------------
	
	private case class _ReachWindowContext(wrapped: WindowContext, cursors: Option[CursorSet],
	                                       revalidationStyle: RevalidationStyle, customDrawers: Vector[CustomDrawer],
	                                       getAnchor: (ReachCanvas2, Bounds) => Point)
		extends ReachWindowContext
	{
		override def self: ReachWindowContext = this
		
		override def withCursors(cursors: Option[CursorSet]): ReachWindowContext = copy(cursors = cursors)
		override def withRevalidationStyle(style: RevalidationStyle): ReachWindowContext =
			copy(revalidationStyle = style)
		override def withCustomDrawers(customDrawers: Vector[CustomDrawer]): ReachWindowContext =
			copy(customDrawers = customDrawers)
		override def withGetAnchor(getAnchor: (ReachCanvas2, Bounds) => Point): ReachWindowContext =
			copy(getAnchor = getAnchor)
		
		override def withBase(base: WindowContext): ReachWindowContext = copy(wrapped = base)
	}
}

/**
  * Context that specifies information for creating Reach window instances
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
trait ReachWindowContext extends ReachWindowContextLike[ReachWindowContext] with WindowContext