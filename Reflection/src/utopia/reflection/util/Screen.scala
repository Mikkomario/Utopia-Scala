package utopia.reflection.util

import utopia.genesis.shape.shape2D.Size
import java.awt.Toolkit
import java.awt.GraphicsConfiguration
import utopia.reflection.shape.Insets

/**
* This object represents the primary display used
* @author Mikko Hilpinen
* @since 25.3.2019
**/
object Screen
{
    // TODO: Handle cases where there is no screen
    
    /**
     * The current size of the screen
     */
	def size = Size of Toolkit.getDefaultToolkit.getScreenSize
	
	/**
	 * @return Current width of the screen
	 */
	def width = size.width
	
	/**
	 * @return Current height of the screen
	 */
	def height = size.height
	
	/**
	 * The insets of this screen in the specified graphics configuration
	 * @param configuration the graphics configuration where the insets are read
	 */
	def insetsAt(configuration: GraphicsConfiguration) = Insets of Toolkit.getDefaultToolkit.getScreenInsets(configuration)
}