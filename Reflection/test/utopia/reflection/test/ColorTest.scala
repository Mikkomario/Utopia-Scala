package utopia.reflection.test

import utopia.paradigm.color.Color
import utopia.reflection.color.ColorSet
import utopia.reflection.color.TextColorStandard.{Dark, Light}

/**
  * Tests some color-related functions
  * @author Mikko Hilpinen
  * @since 28.1.2021, v2
  */
object ColorTest extends App
{
	assert(Color.black.relativeLuminance == 0.0)
	assert(Color.white.relativeLuminance == 1.0)
	
	val colors = ColorSet.fromHexes("#2962ff", "#768fff", "#0039cb").get
	colors.values.foreach { c => println(c.luminosity) }
	println()
	colors.values.foreach { c => println(c.relativeLuminance) }
	println()
	
	println(colors.light.contrastAgainst(Color.white))
	println(colors.light.contrastAgainst(Color.black))
	
	assert(colors.light.textColorStandard == Dark)
	assert(colors.dark.textColorStandard == Light)
	assert(colors.default.textColorStandard == Light)
	
	println("Success!")
}
