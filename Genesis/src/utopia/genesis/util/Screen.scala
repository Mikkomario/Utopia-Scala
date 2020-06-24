package utopia.genesis.util

import java.awt.{GraphicsConfiguration, Toolkit}

import utopia.genesis.shape.shape2D.{Insets, Size}

import scala.util.Try

/**
* This object represents the primary display used
* @author Mikko Hilpinen
* @since 25.3.2019
**/
object Screen
{
	// COMPUTED ---------------------------
	
    /**
     * The current size of the screen (zero size if there is no screen)
     */
	def size = Try { Size of toolkit.getScreenSize }.getOrElse(Size.zero)
	
	/**
	 * @return Current width of the screen (0 when there is no screen)
	 */
	def width = size.width
	
	/**
	 * @return Current height of the screen (0 when there is no screen)
	 */
	def height = size.height
	
	/**
	 * @return Pixels per inch resolution of the screen (zero ppi when there is no screen)
	 */
	def ppi = Try { Ppi(toolkit.getScreenResolution) }.getOrElse(Ppi.zero)
	
	private def toolkit = Toolkit.getDefaultToolkit
	
	
	// OTHER    --------------------------
	
	/**
	 * The insets of this screen in the specified graphics configuration
	 * @param configuration the graphics configuration where the insets are read
	 */
	def insetsAt(configuration: GraphicsConfiguration) =
		Try { Insets of toolkit.getScreenInsets(configuration) }.getOrElse(Insets.zero)
}
