package utopia.genesis.util

import utopia.paradigm.measurement
import utopia.paradigm.measurement.{Distance, Ppi}
import utopia.paradigm.shape.shape2d.{Insets, Size, Vector2D}

import java.awt.{GraphicsConfiguration, Toolkit}
import scala.util.Try

/**
* This object represents the primary display used
* @author Mikko Hilpinen
* @since 25.3.2019
**/
object Screen
{
	// ATTRIBUTES   -----------------------
	
	private var screenSizeMod: Option[Vector2D] = None
	
	private lazy val standardSize = Try { Size(toolkit.getScreenSize) }.getOrElse(Size.zero)
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return The virtual size of this screen. This is solely based on the value provided by the OS and may vary from
	 *         the real size. However, when applying OS scaling, this size is correct.
	 */
	def size = standardSize
	
    /**
     * The actual size of the monitor in pixels. Requires calling 'registerRealScreenSize' in order to return different
     * values.
     */
	def actualSize = {
		val standard = standardSize
		screenSizeMod match {
			case Some(scaling) => standard * scaling
			case None => standard
		}
	}
	
	/**
	 * @return Current width of the screen (0 when there is no screen)
	 */
	def width = size.width
	
	/**
	 * @return Current height of the screen (0 when there is no screen)
	 */
	def height = size.height
	
	/**
	  * @return Pixels per inch resolution of the screen (zero ppi when there is no screen). Takes into account forced
	  *         scaling from the OS, provided that the real screen size has been manually registered.
	  */
	implicit def ppi: Ppi = {
		val base = Try { measurement.Ppi(toolkit.getScreenResolution) }.getOrElse(measurement.Ppi.zero)
		screenSizeMod match {
			case Some(scaling) => base / scaling.maxDimension
			case None => base
		}
	}
	
	private def toolkit = Toolkit.getDefaultToolkit
	
	
	// OTHER    --------------------------
	
	/**
	  * @param pixelsCount Amount of pixels
	  * @return A distance based on that amount of pixels on this screen
	  */
	def pixels(pixelsCount: Double) = Distance.ofPixels(pixelsCount)(ppi)
	
	/**
	 * The insets of this screen in the specified graphics configuration. This value is correct only when applying
	  * OS forced scaling. The actual pixel length of the insets may vary.
	 * @param configuration the graphics configuration where the insets are read
	 */
	def insetsAt(configuration: GraphicsConfiguration) = Try { Insets of toolkit.getScreenInsets(configuration) }
		.getOrElse(Insets.zero)
	
	/**
	  * The insets of this screen in the specified graphics configuration. This is the actual pixel size of the insets,
	  * but forced OS scaling may alter its use.
	  * @param configuration the graphics configuration where the insets are read
	  */
	def actualInsetsAt(configuration: GraphicsConfiguration) = {
		val standard = insetsAt(configuration)
		screenSizeMod match {
			case Some(scaling) => standard * scaling
			case None => standard
		}
	}
	
	/**
	 * Used for informing this object about the actual screen pixel size. Sometimes Windows uses scaling which
	 * messes up all inside-application settings that are based on the screen size. This method can be used to partially
	 * remedy that.
	 * @param realSize A custom screen size
	 */
	def registerRealScreenSize(realSize: Size) = screenSizeMod = Some((realSize / standardSize).toVector)
}
