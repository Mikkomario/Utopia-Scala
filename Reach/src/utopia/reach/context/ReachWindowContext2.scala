package utopia.reach.context

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.context.window.{WindowContext2, WindowContextWrapper2}
import utopia.genesis.handling.action.ActorHandler
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.container.RevalidationStyle.Immediate
import utopia.reach.container.{ReachCanvas, RevalidationStyle}
import utopia.reach.cursor.CursorSet

import scala.language.implicitConversions

object ReachWindowContext2
{
	// OTHER    -----------------------
	
	/**
	  * Creates a new reach window context
	  * @param base Window creation context to wrap
	  * @param background Background color to use in the created windows
	  * @param cursors Set of cursors to use (optional)
	  * @param revalidationStyle Revalidation style to use (default = revalidate immediately in a separate thread)
	  * @param getAnchor Anchoring function (default = anchor over the focused component, or at the window center)
	  * @return A new reach window context
	  */
	def apply(base: WindowContext2, background: Color, cursors: Option[CursorSet] = None,
	          revalidationStyle: RevalidationStyle = Immediate.async,
	          getAnchor: (ReachCanvas, Bounds) => Point = _.anchorPosition(_)): ReachWindowContext2 =
		_ReachWindowContext(base, background, cursors, revalidationStyle, getAnchor)
	
	/**
	  * @param actorHandler The actor handler to wrap
	  * @param background Window background color
	  * @return A Reach window context that uses the specified actor handler and default settings
	  */
	def apply(actorHandler: ActorHandler, background: Color): ReachWindowContext2 =
		apply(WindowContext2(actorHandler), background)
	
	
	// NESTED   -----------------------
	
	private case class _ReachWindowContext(windowContext: WindowContext2, windowBackground: Color,
	                                       cursors: Option[CursorSet], revalidationStyle: RevalidationStyle,
	                                       getAnchor: (ReachCanvas, Bounds) => Point)
		extends ReachWindowContext2 with WindowContextWrapper2[WindowContext2, ReachWindowContext2]
	{
		override def self = this
		
		override def transparent =
			super.transparent.mapWindowBackground { _.withAlpha(0.0) }
		
		override def withWindowBackground(bg: Color) = copy(windowBackground = bg)
		override def withCursors(cursors: Option[CursorSet]) = copy(cursors = cursors)
		override def withRevalidationStyle(style: RevalidationStyle) =
			copy(revalidationStyle = style)
		override def withGetAnchor(getAnchor: (ReachCanvas, Bounds) => Point) =
			copy(getAnchor = getAnchor)
		
		override def withWindowContext(base: WindowContext2) =
			if (base == windowContext) self else copy(windowContext = base)
		
		override def withContentContext(textContext: StaticTextContext) =
			StaticReachContentWindowContext(this, textContext)
	}
}

/**
  * Context that specifies information for creating Reach window instances
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
trait ReachWindowContext2
	extends WindowContext2 with ReachWindowContextCopyable[ReachWindowContext2, StaticReachContentWindowContext]