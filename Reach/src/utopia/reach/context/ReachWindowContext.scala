package utopia.reach.context

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.context.window.{WindowContext, WindowContextWrapper}
import utopia.genesis.handling.action.ActorHandler
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.container.RevalidationStyle.Immediate
import utopia.reach.container.{ReachCanvas, RevalidationStyle}
import utopia.reach.cursor.CursorSet

import scala.language.implicitConversions

object ReachWindowContext
{
	// OTHER    -----------------------
	
	/**
	  * Creates a new reach window context
	  * @param base Window creation context to wrap
	  * @param background Background color to use in the created windows
	  * @param cursors Set of cursors to use (optional)
	  * @param revalidationStyle Revalidation style to use (default = revalidate immediately in a separate thread)
	  * @return A new reach window context
	  */
	def apply(base: WindowContext, background: Color, cursors: Option[CursorSet] = None,
	          revalidationStyle: RevalidationStyle = Immediate.async): ReachWindowContext =
		_ReachWindowContext(base, background, cursors, revalidationStyle, Some(_.anchorPosition(_)), None)
	
	/**
	  * @param actorHandler The actor handler to wrap
	  * @param background Window background color
	  * @return A Reach window context that uses the specified actor handler and default settings
	  */
	def apply(actorHandler: ActorHandler, background: Color): ReachWindowContext =
		apply(WindowContext(actorHandler), background)
	
	
	// NESTED   -----------------------
	
	private case class _ReachWindowContext(windowContext: WindowContext, windowBackground: Color,
	                                       cursors: Option[CursorSet], revalidationStyle: RevalidationStyle,
	                                       getAnchor: Option[(ReachCanvas, Bounds) => Point],
	                                       positionAfterResize: Option[Bounds => Point])
		extends ReachWindowContext with WindowContextWrapper[WindowContext, ReachWindowContext]
	{
		override def self = this
		
		override def transparent =
			super.transparent.mapWindowBackground { _.withAlpha(0.0) }
		
		override def withWindowBackground(bg: Color) = copy(windowBackground = bg)
		override def withCursors(cursors: Option[CursorSet]) = copy(cursors = cursors)
		override def withRevalidationStyle(style: RevalidationStyle) =
			copy(revalidationStyle = style)
		// Setting the anchor disables custom-positioning and vise versa
		override def withGetAnchor(getAnchor: Option[(ReachCanvas, Bounds) => Point]) =
			copy(getAnchor = getAnchor, positionAfterResize = if (getAnchor.isDefined) None else positionAfterResize)
		override def withPositionAfterResize(f: Option[Bounds => Point]): ReachWindowContext =
			copy(positionAfterResize = f, getAnchor = if (f.isDefined) None else getAnchor)
		
		override def withWindowContext(base: WindowContext) =
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
trait ReachWindowContext
	extends WindowContext with ReachWindowContextCopyable[ReachWindowContext, StaticReachContentWindowContext]