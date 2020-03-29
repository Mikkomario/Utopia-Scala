package utopia.reflection.color

import utopia.genesis.color.Color

/**
 * Specifies the standard or default text color so that its legible on a specific background
 * @author Mikko Hilpinen
 * @since 15.1.2020, v1
 */
sealed trait TextColorStandard
{
	/**
	 * @return Default / recommended text color for this standard
	 */
	def defaultTextColor: Color
}

object TextColorStandard
{
	/**
	 * Standard used when text needs to be dark (displayed on light background)
	 */
	case object Dark extends TextColorStandard
	{
		override def defaultTextColor = Color.textBlack
	}
	
	/**
	 * Standard used when text needs to be light (displayed on dark background)
	 */
	case object Light extends TextColorStandard
	{
		override def defaultTextColor = Color.white
	}
}