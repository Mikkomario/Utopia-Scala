package utopia.reflection.component.reach.template

import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.reflection.cursor.{Cursor, CursorType}

/**
  * A common trait for components that specify what type of cursor should be used inside their bounds
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
trait CursorDefining
{
	/**
	  * @return The type of cursor displayed over this component
	  */
	def cursorType: CursorType
	
	/**
	  * @return The area <b>relative to the cursor managed context</b> defined by this component. Usually this means
	  *         the component's location and size in relation to the managed canvas element.
	  */
	def cursorBounds: Bounds
	
	/**
	  * Determines the image to use for the cursor
	  * @param cursor Proposed cursor data
	  * @param position Position of the cursor, relative to this component's cursorBounds property
	  * @return The image to draw as the cursor over that location
	  */
	def cursorToImage(cursor: Cursor, position: Point): Image
}
