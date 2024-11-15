package utopia.reach.context

import utopia.firmament.context.window.WindowContextPropsView
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.container.{ReachCanvas, RevalidationStyle}
import utopia.reach.cursor.CursorSet

/**
  * Common trait for context instances that provide information for creating ReachWindows.
  * This trait only defines properties for reading, not for copying.
  * It does not limit the implementation to either a static or a variable approach.
  * @author Mikko Hilpinen
  * @since 14.11.2024, v1.5
  */
trait ReachWindowContextPropsView extends WindowContextPropsView
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
}
