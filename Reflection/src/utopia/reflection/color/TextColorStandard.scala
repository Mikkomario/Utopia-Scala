package utopia.reflection.color

import utopia.paradigm.color.Color

/**
 * Specifies the standard or default text color so that its legible on a specific background
 * @author Mikko Hilpinen
 * @since 15.1.2020, v1
 */
@deprecated("Replaced with ColorShade in Paradigm", "v2.0")
trait TextColorStandard
{
	/**
	 * @return Default / recommended text color for this standard
	 */
	def defaultTextColor: Color
	
	/**
	  * @return Color used for hints & disabled text
	  */
	def hintTextColor: Color = defaultTextColor.timesAlpha(0.625)
}

@deprecated("Replaced with ColorShade in Paradigm", "v2.0")
object TextColorStandard
{
	/**
	 * Standard used when text needs to be dark (displayed on light background)
	 */
	@deprecated("Replaced with ColorShade.Light in Paradigm", "v2.0")
	case object Dark extends TextColorStandard
	{
		override def defaultTextColor = Color.textBlack
	}
	
	/**
	 * Standard used when text needs to be light (displayed on dark background)
	 */
	@deprecated("Replaced with ColorShade.Dark in Paradigm", "v2.0")
	case object Light extends TextColorStandard
	{
		override def defaultTextColor = Color.textWhite
	}
}