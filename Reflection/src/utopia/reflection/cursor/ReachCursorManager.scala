package utopia.reflection.cursor

import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.reach.template.CursorDefining

/**
  * Used for determining, which cursor image should be drawn
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
class ReachCursorManager(cursors: CursorSet)
{
	// ATTRIBUTES	-----------------------------
	
	private var cursorComponents = Vector[CursorDefining]()
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param position A position in the cursor managed context
	  * @return Cursor image to use over that position
	  */
	def cursorAt(position: Point) = cursorComponents.find { _.cursorBounds.contains(position) } match
	{
		case Some(component) => cursors(component.cursor)
		case None => cursors.default
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
