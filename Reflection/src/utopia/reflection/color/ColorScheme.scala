package utopia.reflection.color

import utopia.genesis.color.RGB

/**
  * Defines program default colors
  * @author Mikko Hilpinen
  * @since 17.11.2019, v1
  * @param primary The primary color's used
  * @param secondary The secondary color's used
  * @param gray suplementary grayscale colors used
 *  @param error Color used in error situations
  */
// TODO: Use an enumeration here as well
case class ColorScheme(primary: ColorSet, secondary: ColorSet, gray: ColorSet = ColorSet(RGB.grayWithValue(225),
	RGB.grayWithValue(245), RGB.grayWithValue(225)), error: ComponentColor = RGB.withValues(176, 0, 32))
