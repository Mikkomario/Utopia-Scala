package utopia.reach.context

import utopia.firmament.context.{BaseContext, TextContext, WindowContextWrapper}
import utopia.flow.collection.immutable.range.Span
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.container.RevalidationStyle.{Delayed, Immediate}
import utopia.reach.container.{ReachCanvas, RevalidationStyle}
import utopia.reach.cursor.CursorType.{Default, Interactive, Text}
import utopia.reach.cursor.{Cursor, CursorSet, CursorType}

import scala.concurrent.duration.FiniteDuration

/**
  * Common trait for context instances that provide enough information for creating ReachWindows
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  * @tparam Repr Concrete context implementation
  */
trait ReachWindowContextLike[+Repr, +Textual] extends WindowContextWrapper[Repr]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return The background color used in created windows
	  */
	def windowBackground: Color
	/**
	  * @return Cursors used in created canvases
	  */
	def cursors: Option[CursorSet]
	/**
	  * @return Revalidation style used, which determines how revalidate() is implemented
	  */
	def revalidationStyle: RevalidationStyle
	/**
	  * @return A function that determines the window anchor position.
	  *         Accepts a canvas instance and window bounds.
	  *         Returns a point within or outside the bounds that serves as the window "anchor".
	  */
	def getAnchor: (ReachCanvas, Bounds) => Point
	
	/**
	  * @param bg New background color to use
	  * @return Context copy with specified background color
	  */
	def withWindowBackground(bg: Color): Repr
	/**
	  * @param cursors New set of cursors to use. None if no cursors should be used.
	  * @return A copy of this context with those cursors in use
	  */
	def withCursors(cursors: Option[CursorSet]): Repr
	/**
	  * @param style New component revalidation style
	  * @return Context copy with that style in use
	  */
	def withRevalidationStyle(style: RevalidationStyle): Repr
	/**
	  * @param getAnchor An anchoring function
	  * @return Context copy that uses the specified anchoring function
	  */
	def withGetAnchor(getAnchor: (ReachCanvas, Bounds) => Point): Repr
	
	/**
	  * @param textContext A text context to add to this context
	  * @return A copy of this context with the specified textual context in place
	  */
	def withContentContext(textContext: TextContext): Textual
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this context that doesn't use cursors
	  */
	def withoutCursors = withCursors(None)
	
	/**
	  * @return A copy of this context where revalidation happens immediately (without a delay).
	  *         The revalidation might block or be asynchronous, based on the previously used style.
	  */
	def revalidatingImmediately = mapRevalidationStyle {
		case _: Delayed => Immediate.async
		case o => o
	}
	/**
	  * @return A copy of this context where revalidation is asynchronous.
	  *         The revalidation might be immediate or delayed, based on the previously used style.
	  */
	def revalidatingAsync = mapRevalidationStyle {
		case i: Immediate => i.async
		case o => o
	}
	
	/**
	  * @return Context copy that anchors the windows on the focused component,
	  *         or at the window center, if there is no focused component
	  */
	def withFocusAnchoring = withGetAnchor { _.anchorPosition(_) }
	/**
	  * @return Context copy that anchors the windows on their center points
	  */
	def anchoringToCenter = withAnchorAlignment(Alignment.Center)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param f A mapping function for background color
	  * @return Context copy with mapped window background
	  */
	def mapWindowBackground(f: Color => Color) = withWindowBackground(f(windowBackground))
	
	def mapRevalidationStyle(f: RevalidationStyle => RevalidationStyle) =
		withRevalidationStyle(f(revalidationStyle))
	
	/**
	  * @param cursors New set of cursors to use
	  * @return Copy of this context with those cursors in use
	  */
	def withCursors(cursors: CursorSet): Repr = withCursors(Some(cursors))
	def withCursor(cursorType: CursorType, cursor: Cursor) = {
		val newCursors = cursors match {
			case Some(existing) => existing + (cursorType -> cursor)
			case None => CursorSet(Map(cursorType -> cursor), cursor)
		}
		withCursors(newCursors)
	}
	/**
	  * @param cursor New cursor to use (style -> cursor)
	  * @return Context copy with that cursor in use
	  */
	def +(cursor: (CursorType, Cursor)) = withCursor(cursor._1, cursor._2)
	/**
	  * @param cursor New interactive cursor (i.e. the hand cursor)
	  * @return Copy of this context with that cursor in use
	  */
	def withInteractionCursor(cursor: Cursor) = withCursor(Interactive, cursor)
	/**
	  * @param cursor New cursor to display over text areas
	  * @return Copy of this context with that cursor in use
	  */
	def withTextCursor(cursor: Cursor) = withCursor(Text, cursor)
	/**
	  * @param cursor The default cursor (typically the arrow cursor)
	  * @return Copy of this context where that cursor is the default cursor
	  */
	def withDefaultCursor(cursor: Cursor) = {
		val newCursors = cursors match {
			case Some(existing) => CursorSet(existing.cursors + (Default -> cursor), cursor)
			case None => CursorSet(Map(Default -> cursor), cursor)
		}
		withCursors(newCursors)
	}
	def withoutCursor(cursor: CursorType) = withCursors(cursors.map { _ - cursor })
	def -(cursor: CursorType) = withoutCursor(cursor)
	
	/**
	  * @param delay Minimum and maximum revalidation delays
	  * @return Context copy that performs revalidation between the specified delays
	  */
	def revalidatingAfter(delay: Span[FiniteDuration]) = withRevalidationStyle(Delayed(delay.ascending))
	/**
	  * @param delay A fixed revalidation delay
	  * @return Context copy that performs revalidation after the specified delay
	  */
	def revalidatingAfter(delay: FiniteDuration): Repr = revalidatingAfter(Span(delay, delay))
	/**
	  * @param min Minimum revalidation delay
	  * @param max Maximum revalidation delay
	  * @return Context copy that performs revalidation between the specified delays
	  */
	def revalidatingAfter(min: FiniteDuration, max: FiniteDuration): Repr = revalidatingAfter(Span(min, max))
	
	/**
	  * @param alignment Alignment, around which windows should be anchored.
	  *                  For example, if specifying Alignment.Left, windows will expand to the right and equally to
	  *                  top and bottom, but not to the left.
	  * @return Context copy with anchoring logic using that alignment
	  */
	def withAnchorAlignment(alignment: Alignment) = withGetAnchor { (_, b) => alignment.origin(b) }
	
	/**
	  * @param context Context to use for creating window contents
	  * @return A copy of this context that can create window content, also
	  */
	def withContentContext(context: BaseContext): Textual =
		withContentContext(context.against(windowBackground).forTextComponents: TextContext)
	/**
	  * @param textContext A text context to add to this window context
	  * @return A context suitable for creating popup windows
	  */
	def ++(textContext: TextContext) = withContentContext(textContext)
}
