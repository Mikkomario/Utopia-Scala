package utopia.reflection.cursor

import utopia.genesis.color.Color
import utopia.genesis.image.Image

/**
  * Cursors specify the image that should be drawn at the mouse cursor location
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
trait Cursor
{
	/**
	  * @param color Color of the element / pixel below the cursor
	  * @return Cursor image to display over that color
	  */
	def over(color: Color): Image
}
