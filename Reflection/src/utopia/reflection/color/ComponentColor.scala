package utopia.reflection.color

import scala.language.implicitConversions
import utopia.genesis.color.Color

/**
 * Represents a color used as a component background
 * @author Mikko Hilpinen
 * @since 15.1.2020, v1
 * @param background The background color for target components
 * @param textColorStandard The limitations & recommendations applied to text color on this background
 */
case class ComponentColor(background: Color, textColorStandard: TextColorStandard)
{
	// COMPUTED	------------------------------
	
	/**
	 * @return A default text color used with this component coloring
	 */
	def defaultTextColor: Color = textColorStandard.defaultTextColor
	
	/**
	  * @return A highlighted copy of this component color
	  */
	def highlighted =
	{
		// Either darkens or lightens the color, depending on original color lightness
		val newBG =
		{
			if (background.luminosity <= 0.65)
				background.lightened(1.6)
			else
				background.darkened(1.6)
		}
		copy(background = newBG)
	}
	
	/**
	  * @return A color set that consists only of this one color
	  */
	def invariable = ColorSet(this, this, this)
	
	
	// IMPLEMENTED	--------------------------
	
	override def toString = background.toString
}

object ComponentColor
{
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
	implicit def autoGenerate(color: Color): ComponentColor = ComponentColor(color,
		if (color.luminosity < 0.35) TextColorStandard.Light else TextColorStandard.Dark)
	
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