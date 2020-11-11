package utopia.reflection.component.reach.template

import utopia.genesis.shape.shape2D.Bounds
import utopia.reflection.cursor.CursorType

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
	def cursor: CursorType
	
	/**
	  * @return The area <b>relative to the cursor managed context</b> defined by this component. Usually this means
	  *         the component's location and size in relation to the managed canvas element.
	  */
	def cursorBounds: Bounds
}
