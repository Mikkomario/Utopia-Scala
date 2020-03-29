package utopia.genesis.test

import utopia.genesis.color.RGBChannel._
import utopia.genesis.color.{Color, HSL, RGB}
import utopia.genesis.shape.Angle

/**
  * This app programmatically tests color conversion
  * @author Mikko Hilpinen
  * @since 26.4.2019, v2.1+
  */
object BasicColorTest extends App
{
	// Tests RGB
	val rgb1 = RGB(0.6, 0.3, 0.3)
	
	assert( rgb1 + Red(0.1) - Red(0.1) == rgb1)
	assert( rgb1 * 0.5 + rgb1 * 0.5 == rgb1 )
	assert( rgb1 / 2 == rgb1 * 0.5 )
	assert( rgb1.inverted == RGB(0.4, 0.7, 0.7) )
	assert( rgb1.minRatio == 0.3 )
	assert( rgb1.maxRatio == 0.6 )
	assert( rgb1.withRatio(Blue, 0.7) == RGB(0.6, 0.3, 0.7) )
	
	// Tests HSL
	val hsl1 = HSL(0, 0.5, 0.5)
	
	assert( hsl1.withLuminosity(0.2) == HSL(0, 0.5, 0.2) )
	assert( hsl1.withSaturation(0.8) == HSL(0, 0.8, 0.5))
	assert( hsl1.withHue(Angle.ofDegrees(120)) ~== HSL(120, 0.5, 0.5))
	assert( hsl1 + 200 - 200 == hsl1 )
	
	println(s"$hsl1 -> ${ hsl1 + 540 } -> ${ hsl1 + 540 - 540 }")
	
	assert( hsl1 + 540 - 540 == hsl1 )
	assert( hsl1.grayscale.saturation == 0 )
	assert( hsl1.plusHueTowards(30, 90) == HSL(30, 0.5, 0.5) )
	assert( hsl1.plusHueTowards(30, 270) == HSL(330, 0.5, 0.5) )
	
	// Tests conversion
	val rgb1hsl = rgb1.toHSL
	val rgb1hslrgb = rgb1hsl.toRGB
	
	println(s"$rgb1 -> $rgb1hsl -> $rgb1hslrgb")
	assert( rgb1.toHSL.toRGB ~== rgb1 )
	
	val hsl1rgb = hsl1.toRGB
	val hsl1rgbhsl = hsl1rgb.toHSL
	
	println(s"$hsl1 -> $hsl1rgb -> $hsl1rgbhsl")
	assert( hsl1.toRGB.toHSL ~== hsl1 )
}
