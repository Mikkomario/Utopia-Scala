package utopia.reflection.color

import scala.language.implicitConversions
import utopia.paradigm.color.Color
import utopia.reflection.color.TextColorStandard.{Dark, Light}

object ComponentColor
{
	// IMPLICIT 	--------------------------
	
	/**
	  * Automatically converts a component color to color
	  * @param componentColor A component color
	  * @return Background color of that component color
	  */
	implicit def unwrapColor(componentColor: ComponentColor): Color = componentColor.background
	
	/**
	  * Automatically generates a legible component color based on provided color
	  * @param color A component background color
	  * @return A component color that should be legible
	  */
	implicit def autoGenerate(color: Color): ComponentColor = ComponentColor(color)
	
	/**
	  * Automatically generates a legible component color based on a provided color-like object
	  * @param color A color-convertible object
	  * @param convert A converting function (implicit)
	  * @tparam A Type of convertible object
	  * @return A component color that should be legible
	  */
	implicit def autoGenerateFromConvertible[A](color: A)(implicit convert: A => Color): ComponentColor =
		autoGenerate(convert(color))
}

/**
 * Represents a color used as a component background
 * @author Mikko Hilpinen
 * @since 15.1.2020, v1
 * @param background The background color for target components
 */
case class ComponentColor(background: Color)
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Standard for text color use on this background
	  */
	lazy val textColorStandard =
	{
		// Picks the text color with greater contrast against the background color
		val whiteContrast = Color.white.contrastAgainst(background)
		val blackContrast = Color.black.contrastAgainst(background)
		
		if (blackContrast >= whiteContrast) Dark else Light
	}
	
	
	// COMPUTED	------------------------------
	
	/**
	 * @return A default text color used with this component coloring
	 */
	def defaultTextColor: Color = textColorStandard.defaultTextColor
	
	/**
	 * @return The overall shade of this color (either darker or lighter)
	 */
	def shade = textColorStandard match {
		case Light => ColorShade.Dark
		case Dark => ColorShade.Light
	}
	
	/**
	  * @return A highlighted copy of this component color
	  */
	def highlighted = copy(background = background.highlighted)
	
	/**
	  * @return A color set that consists only of this one color
	  */
	def invariable = ColorSet(this, this, this)
	
	
	// IMPLEMENTED	--------------------------
	
	override def toString = background.toString
	
	
	// OTHER	------------------------------
	
	/**
	  * @param intensity Amount of highlighting to do, where 0 is no highlighting and 1.0 is the default highlighting
	  * @return A modified version of this color
	  */
	def highlightedBy(intensity: Double) = copy(background = background.highlightedBy(intensity))
}