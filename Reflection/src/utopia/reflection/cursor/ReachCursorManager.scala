package utopia.reflection.cursor

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.reflection.color.ColorShadeVariant
import utopia.reflection.component.reach.template.CursorDefining

/**
  * Used for determining, which cursor image should be drawn
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
class ReachCursorManager(val cursors: CursorSet)
{
	// ATTRIBUTES	-----------------------------
	
	private var cursorComponents = Vector[CursorDefining]()
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param position A position in the cursor managed context
	  * @param shadeOf A function for calculating the overall shade of the targeted area
	  * @return Cursor image to use over that position
	  */
	def cursorAt(position: Point)(shadeOf: Bounds => ColorShadeVariant) =
	{
		// Checks whether any of the registered components is controlling the specified position
		cursorComponents.findMap { c =>
			val bounds = c.cursorBounds
			if (bounds.contains(position))
				Some(c -> bounds)
			else
				None
		} match
		{
			// Case: Component manages area => lets the component decide cursor styling
			case Some((component, bounds)) =>
				component.cursorToImage(cursors(component.cursorType), position - bounds.position)
			// Case: Cursor is outside all registered component zones => uses default cursor with modified shade
			case None =>
				val cursor = cursors.default
				cursor(shadeOf(cursor.defaultBounds.translated(position)).opposite)
		}
	}
	
	/**
	  * Registers a component to affect the selected cursor
	  * @param component A component to affect cursor selection from now on
	  */
	def registerComponent(component: CursorDefining) = cursorComponents :+= component
	
	/**
	  * Removes a component from affecting the selected cursor
	  * @param component A component to no longer consider when selecting cursor
	  */
	def unregisterComponent(component: Any) = cursorComponents = cursorComponents.filterNot { _ == component }
	
	/**
	  * Registers a component to affect the selected cursor
	  * @param component A component to affect cursor selection from now on
	  */
	def +=(component: CursorDefining) = registerComponent(component)
	
	/**
	  * Removes a component from affecting the selected cursor
	  * @param component A component to no longer consider when selecting cursor
	  */
	def -=(component: Any) = unregisterComponent(component)
}
