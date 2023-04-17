package utopia.reach.context

import utopia.firmament.context.{TextContext, WindowContext}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.container.RevalidationStyle.Immediate
import utopia.reach.container.{ReachCanvas2, RevalidationStyle}
import utopia.reach.cursor.CursorSet
import utopia.firmament.drawing.template.CustomDrawer
import utopia.paradigm.color.Color

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
	  * @param customDrawers Custom drawers to assign to windows (default = empty)
	  * @param getAnchor Anchoring function (default = anchor over the focused component, or at the window center)
	  * @return A new reach window context
	  */
	def apply(base: WindowContext, background: Color, cursors: Option[CursorSet] = None,
	          revalidationStyle: RevalidationStyle = Immediate.async, customDrawers: Vector[CustomDrawer] = Vector(),
	          getAnchor: (ReachCanvas2, Bounds) => Point = _.anchorPosition(_)): ReachWindowContext =
		_ReachWindowContext(base, background, cursors, revalidationStyle, customDrawers, getAnchor)
	
	/**
	  * @param actorHandler The actor handler to wrap
	  * @param background Window background color
	  * @return A Reach window context that uses the specified actor handler and default settings
	  */
	def apply(actorHandler: ActorHandler, background: Color): ReachWindowContext =
		apply(WindowContext(actorHandler), background)
	
	
	// NESTED   -----------------------
	
	private case class _ReachWindowContext(windowContext: WindowContext, background: Color, cursors: Option[CursorSet],
	                                       revalidationStyle: RevalidationStyle, customDrawers: Vector[CustomDrawer],
	                                       getAnchor: (ReachCanvas2, Bounds) => Point)
		extends ReachWindowContext
	{
		override def self: ReachWindowContext = this
		
		override def withBackground(bg: Color): ReachWindowContext = copy(background = bg)
		override def withCursors(cursors: Option[CursorSet]): ReachWindowContext = copy(cursors = cursors)
		override def withRevalidationStyle(style: RevalidationStyle): ReachWindowContext =
			copy(revalidationStyle = style)
		override def withGetAnchor(getAnchor: (ReachCanvas2, Bounds) => Point): ReachWindowContext =
			copy(getAnchor = getAnchor)
		
		override def withWindowContext(base: WindowContext): ReachWindowContext =
			if (base == windowContext) self else copy(windowContext = base)
		
		override def withTextContext(textContext: TextContext): PopupContext = PopupContext(this, textContext)
	}
}

/**
  * Context that specifies information for creating Reach window instances
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
trait ReachWindowContext extends ReachWindowContextLike[ReachWindowContext, PopupContext] with WindowContext