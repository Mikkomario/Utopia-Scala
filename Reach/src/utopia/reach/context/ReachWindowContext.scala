package utopia.reach.context

import utopia.firmament.context.{TextContext, WindowContext}
import utopia.genesis.handling.mutable.ActorHandler
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
	  * @param getAnchor Anchoring function (default = anchor over the focused component, or at the window center)
	  * @return A new reach window context
	  */
	def apply(base: WindowContext, background: Color, cursors: Option[CursorSet] = None,
	          revalidationStyle: RevalidationStyle = Immediate.async,
	          getAnchor: (ReachCanvas, Bounds) => Point = _.anchorPosition(_)): ReachWindowContext =
		_ReachWindowContext(base, background, cursors, revalidationStyle, getAnchor)
	
	/**
	  * @param actorHandler The actor handler to wrap
	  * @param background Window background color
	  * @return A Reach window context that uses the specified actor handler and default settings
	  */
	def apply(actorHandler: ActorHandler, background: Color): ReachWindowContext =
		apply(WindowContext(actorHandler), background)
	
	
	// NESTED   -----------------------
	
	private case class _ReachWindowContext(windowContext: WindowContext, windowBackground: Color, cursors: Option[CursorSet],
	                                       revalidationStyle: RevalidationStyle,
	                                       getAnchor: (ReachCanvas, Bounds) => Point)
		extends ReachWindowContext
	{
		override def self: ReachWindowContext = this
		
		override def transparent =
			super.transparent.mapWindowBackground { _.withAlpha(0.0) }
		
		override def withWindowBackground(bg: Color): ReachWindowContext = copy(windowBackground = bg)
		override def withCursors(cursors: Option[CursorSet]): ReachWindowContext = copy(cursors = cursors)
		override def withRevalidationStyle(style: RevalidationStyle): ReachWindowContext =
			copy(revalidationStyle = style)
		override def withGetAnchor(getAnchor: (ReachCanvas, Bounds) => Point): ReachWindowContext =
			copy(getAnchor = getAnchor)
		
		override def withWindowContext(base: WindowContext): ReachWindowContext =
			if (base == windowContext) self else copy(windowContext = base)
		
		override def withContentContext(textContext: TextContext): ReachContentWindowContext = ReachContentWindowContext(this, textContext)
	}
}

/**
  * Context that specifies information for creating Reach window instances
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
trait ReachWindowContext extends ReachWindowContextLike[ReachWindowContext, ReachContentWindowContext] with WindowContext