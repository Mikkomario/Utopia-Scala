package utopia.paradigm.color

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.equality.ApproxEquals
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.enumeration.RgbChannel
import utopia.paradigm.generic.ParadigmDataType.ColorType

import scala.language.implicitConversions
import scala.util.Try

object Color
{
	// ATTRIBUTES	---------------------
	
	/**
	  * A black color (0, 0, 0)
	  */
	val black = Color(0, 0, 0)
	/**
	  * A white color (1, 1, 1)
	  */
	val white = Color(1, 1, 1)
	/**
	  * A red color (1, 0, 0)
	  */
	val red = Color(1, 0, 0)
	/**
	  * A green color (0, 1, 0)
	  */
	val green = Color(0, 1, 0)
	/**
	  * A blue color (0, 0, 1)
	  */
	val blue = Color(0, 0, 1)
	/**
	  * A yellow color (1, 1, 0)
	  */
	val yellow = Color(1, 1, 0)
	/**
	  * A magenta color (1, 0, 1)
	  */
	val magenta = Color(1, 0, 1)
	/**
	  * A cyan color (0, 1, 1)
	  */
	val cyan = Color(0, 1, 1)
	
	/**
	  * The default black text color
	  */
	val textBlack = black.withAlpha(0.88)
	/**
	  * The default black text color for disabled / hint elements
	  */
	val textBlackDisabled = black.withAlpha(0.6)
	
	/**
	 * The default white text color on dark surfaces
	 */
	val textWhite = white.withAlpha(0.88)
	/**
	 * The default white text color for hint elements
	 */
	val textWhiteDisabled = white.withAlpha(0.6)
	
	/**
	  * A black color which is drawn completely transparent
	  */
	val transparentBlack = black.withAlpha(0.0)
	
	
	// IMPLICITS	---------------------
	
	/**
	  * Implicitly converts an awt color to color
	  * @param awtColor An awt color
	  * @return A color
	  */
	implicit def fromAwt(awtColor: java.awt.Color): Color =
		Color(Right(Rgb.withValues(awtColor.getRed, awtColor.getGreen, awtColor.getBlue)), awtColor.getAlpha / 255.0)
	
	
	// OPERATORS	--------------------
	
	/**
	  * Creates a new color
	  * @param r Red ratio [0, 1]
	  * @param g Green ratio [0, 1]
	  * @param b Blue ratio [0, 1]
	  * @param alpha Alpha ratio [0, 1]
	  * @return A new color
	  */
	def apply(r: Double, g: Double, b: Double, alpha: Double = 1.0): Color =
		Color(Right(Rgb(r, g, b)), 0.0 max alpha min 1.0)
	
	/**
	  * @param rgb An rgb color
	  * @param alpha Alpha value assigned to this color [0, 1]
	  * @return A new color based on the rgb values
	  */
	def apply(rgb: Rgb, alpha: Double): Color = Color(Right(rgb), 0.0 max alpha min 1.0)
	
	/**
	  * @param hsl An hsl color
	  * @param alpha Alpha value assigned to this color [0, 1]
	  * @return A new color based on the hsl values
	  */
	def apply(hsl: Hsl, alpha: Double): Color = Color(Left(hsl), 0.0 max alpha min 1.0)
	
	
	// OTHER	-----------------------
	
	/**
	  * @param luminosity Luminosity factor [0, 1] where 0 is black and 1 is white
	  * @return A grayscale color with specified luminosity
	  */
	def gray(luminosity: Double) = apply(luminosity, luminosity, luminosity)
	
	/**
	  * Converts an rgb-alpha value to a color
	  * @param rgba An rgb value with alpha
	  * @return A color based on the rgb value
	  */
	def fromInt(rgba: Int) = {
		// Extracts the RGB and alpha values from the specified bits
		// Logic is from: https://stackoverflow.com/questions/25761438/understanding-bufferedimage-getrgb-output-values
		//                laplasz's answer
		//                Referenced 31.8.2023
		apply(
			Right(Rgb.withValues((rgba & 0xff0000) >> 16, (rgba & 0xff00) >> 8, rgba & 0xff)),
			((rgba & 0xff000000) >>> 24) / 255.0
		)
	}
	
	/**
	 * Converts a hex value to a color value
	 * @param hex A color hex value (Eg. "#FFFFFF")
	 * @return A color value for the hex. Failure if value couldn't be decoded.
	 */
	def fromHex(hex: String) = Try[Color](java.awt.Color.decode(hex))
	
	/**
	  * @param colors A set of colors
	  * @return The average color
	  */
	def average(colors: Iterable[Color]) = {
		if (colors.isEmpty)
			transparentBlack
		else {
			// Combines the total rgb and alpha values of the colors (weights by color alpha)
			val alpha = colors.iterator.map { _.alpha }.sum / colors.size
			if (alpha == 0.0)
				Rgb.average(colors.map { _.rgb }).withAlpha(alpha)
			else
				Rgb.weighedAverage(colors.map { c => c.rgb -> c.alpha }).withAlpha(alpha)
		}
	}
	/**
	 * @param colors A set of colors, each with a weight modifier assigned to it
	 * @return A weighed average between the specified colors
	 */
	def weighedAverage(colors: Iterable[(Color, Double)]) = {
		// Converts all weights to positive by inverting some of the colors
		val correctSignColors = colors.filter { _._2 != 0.0 }
			.map { case (c, wt) => if (wt < 0.0) c.inverted -> (-wt) else c -> wt }
		val totalWeight = correctSignColors.iterator.map { _._2 }.sum
		if (totalWeight == 0.0)
			average(colors.map { _._1 })
		else {
			val alpha = correctSignColors.iterator.map { case (color, wt) => color.alpha * wt }.sum / totalWeight
			Rgb.weighedAverage(correctSignColors.map { case (c, wt) => c.rgb -> (c.alpha * wt) }).withAlpha(alpha)
		}
	}
	
	/**
	  * @param colors A collection of colors
	  * @return The average luminosity of those colors
	  */
	def averageLuminosityOf(colors: IterableOnce[Color]) = colors.iterator
		.map { c => (c.luminosity * c.alpha) -> c.alpha }
		.reduceOption { (a, b) => (a._1 + b._1) -> (a._2 + b._2) } match {
			case Some((totalLuminosity, totalAlpha)) => totalLuminosity / totalAlpha
			case None => 0.0
		}
	
	/**
	  * @param colors A collection of colors
	  * @return The average relative luminance of those colors
	  */
	def averageRelativeLuminanceOf(colors: IterableOnce[Color]) = colors.iterator
		.map { c => (c.relativeLuminance * c.alpha) -> c.alpha }
		.reduceOption { (a, b) => (a._1 + b._1) -> (a._2 + b._2) } match {
			case Some((totalLuminance, totalAlpha)) => totalLuminance / totalAlpha
			case None => 0.0
		}
}

/**
  * This class represents color with either RGB or HSL
  * @author Mikko Hilpinen
  * @since Genesis 24.4.2019, v1+
  */
case class Color private(private val data: Either[Hsl, Rgb], alpha: Double)
	extends RgbLike[Color] with HslLike[Color] with ApproxEquals[Color] with ValueConvertible
{
	// ATTRIBUTES	----------------------
	
	/**
	  * A HSL representation of this color
	  */
	lazy val hsl: Hsl = data.fold(c => c, _.toHSL)
	/**
	  * An RGB representation of this color
	  */
	lazy val rgb: Rgb = data.fold(_.toRGB, c => c)
	
	// Relative luminance is lazily cached
	override lazy val relativeLuminance = super.relativeLuminance
	
	// Shade is cached, also
	override lazy val shade = super.shade
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Whether this color has transparency (can be seen through partially or fully)
	  */
	def transparent = alpha < 1
	/**
	  * @return Whether this color is fully opaque (100% alpha - I.e. can't be seen through)
	  */
	def opaque = !transparent
	/**
	  * @return Whether this color is visible (i.e. is not fully transparent)
	  */
	def visible = alpha > 0
	/**
	  * @return Whether this color is not visible (i.e. is fully transpared)
	  */
	def invisible = !visible
	
	/**
	  * @return The alpha percentage of this color [0, 100]
	  */
	def alphaPercentage = (alpha * 100).toInt
	
	/**
	  * @return An awt representation of this color
	  */
	def toAwt = new java.awt.Color(rgb.redValue, rgb.greenValue, rgb.blueValue, (alpha * 255).toInt)
	
	/**
	  * @return An integer representation of this color. Contains rgb and alpha data.
	  */
	def toInt = toAwt.getRGB
	
	/**
	  * @return A lighter copy of this color
	  */
	def lightened = lightenedBy(1.0)
	/**
	  * @return A darker copy of this color
	  */
	def darkened = darkenedBy(1.0)
	/**
	  * @return Either a lighter or a darker copy of this color, depending on the visual luminance of this color
	  */
	def highlighted = highlightedBy(1.0)
	
	
	// IMPLEMENTED	----------------------
	
	override def self: Color = this
	
	override implicit def toValue: Value = new Value(Some(this), ColorType)
	
	override def ~==(other: Color) = {
		// Alphas must match
		if (alpha ~== other.alpha) {
			// Usually tests with RGB
			if (data.isRight || other.data.isRight)
				rgb ~== other.rgb
			else
				hsl ~== other.hsl
		}
		else
			false
	}
	
	override def ratios = rgb.ratios
	
	override def withRatios(newRatios: Map[RgbChannel, Double]) = withRGB(Rgb.withRatios(newRatios))
	
	override def hue = hsl.hue
	
	override def saturation = hsl.saturation
	
	override def luminosity = hsl.luminosity
	
	def withHue(hue: Angle) = withHSL(hsl.withHue(hue))
	
	def withSaturation(saturation: Double) = withHSL(hsl.withSaturation(saturation))
	
	def withLuminosity(luminosity: Double) = withHSL(hsl.withLuminosity(luminosity))
	
	override def contrastAgainst(other: RgbLike[_]) = {
		// Takes alpha value into account
		if (transparent) {
			val rawContrast = super.contrastAgainst(other)
			// Multiplies the contrast above the 1.0 (same color) level by alpha
			(rawContrast - 1.0) * alpha + 1.0
		}
		else
			super.contrastAgainst(other)
	}
	
	override def toString = {
		val base = data.fold(_.toString, _.toString)
		if (transparent)
			s"$base, $alphaPercentage% Alpha"
		else
			base
	}
	
	
	// OTHER	---------------------------
	
	/**
	  * @param newAlpha New alpha value [0, 1]
	  * @return A copy of this color with specified alpha
	  */
	def withAlpha(newAlpha: Double) = copy(alpha = 0.0 max newAlpha min 1.0)
	
	/**
	  * @param mod An alpha modifier
	  * @return A copy of this color with modified alpha
	  */
	def timesAlpha(mod: Double) = withAlpha(alpha * mod)
	
	/**
	  * @param f A mapping function
	  * @return A copy of this color with mapped alpha
	  */
	def mapAlpha(f: Double => Double) = withAlpha(f(alpha))
	
	/**
	  * @param hsl A new HSL value
	  * @return A copy of this color with provided HSL value
	  */
	def withHSL(hsl: Hsl) = copy(data = Left(hsl))
	
	/**
	  * @param rgb A new RGB value
	  * @return A copy of this color with provided RGB value
	  */
	def withRGB(rgb: Rgb) = copy(data = Right(rgb))
	
	/**
	  * @param f A mapping function
	  * @return A copy of this color with mapped HSL
	  */
	def mapHSL(f: Hsl => Hsl) = withHSL(f(hsl))
	
	/**
	  * @param f a mapping function
	  * @return A copy of this color with mapped RGB
	  */
	def mapRGB(f: Rgb => Rgb) = withRGB(f(rgb))
	
	/**
	  * @param other Another color
	  * @return An average between these colors (rgb-wise)
	  */
	def average(other: Color) = {
		if (other.alpha == 0.0) {
			if (alpha == 0.0)
				Color(Right(rgb.average(other.rgb)), 0.0)
			else
				timesAlpha(0.5)
		}
		else if (alpha == 0.0)
			other.timesAlpha(0.5)
		else
			Color(Right(rgb.average(other.rgb, alpha / other.alpha)), (alpha + other.alpha) / 2)
	}
	/**
	  * @param other Another color
	  * @param weight A weight modifier for <b>this</b> color
	  * @return A weighted average between these colors (rgb-wise)
	  */
	def average(other: Color, weight: Double) = {
		def newAlpha = (alpha * weight + other.alpha) / (1 + weight)
		if (other.alpha == 0.0) {
			if (alpha == 0.0)
				Color(Right(rgb.average(other.rgb, weight)), 0.0)
			else
				withAlpha(newAlpha)
		}
		else if (alpha == 0.0)
			other.withAlpha(newAlpha)
		else
			Color(Right(rgb.average(other.rgb, weight * alpha / other.alpha)), newAlpha)
	}
	
	/**
	  * @param other Another color
	  * @param myWeight A weight modifier for THIS color
	  * @param theirWeight A weight modifier for the other color
	  * @return A weighted average between these colors (rgb-wise)
	  */
	def average(other: Color, myWeight: Double, theirWeight: Double): Color = average(other, myWeight / theirWeight)
	
	/**
	  * Creates a darkened copy of this color
	  * @param impact Impact modifier, where 0 is no impact and 1 is the default impact
	  * @return A darkened copy of this color
	  */
	def darkenedBy(impact: Double) = {
		val mag = 1 + (1 - relativeLuminance) * 2.5
		val t = luminosity * Math.pow(1 - 0.1 * mag, impact) - impact * 0.005
		withLuminosity(t)
	}
	/**
	  * Creates a lighter copy of this color
	  * @param impact Impact modifier, where 0  is no impact and 1 is the default impact
	  * @return A lighter version of this color
	  */
	def lightenedBy(impact: Double) = {
		val mag = 1 + relativeLuminance * 3
		// val g2 = gradient * Math.pow(1 + (1 - o) * 0.5, impact)
		val t = darkness * Math.pow(1 - 0.1 * mag, impact) - impact * 0.01
		withDarkness(t)
	}
	/**
	  * Creates a slightly modified copy of this color
	  * @param impact An impact modifier, where 0 is no impact and 1 is the default impact
	  * @return Either a darker or a lighter version of this color,
	  *         depending on the visual luminance of this color
	  */
	def highlightedBy(impact: Double) = {
		if (relativeLuminance > 0.5)
			darkenedBy(impact)
		else
			lightenedBy(impact)
	}
	/**
	  * Creates a slightly modified copy of this color
	  * @param impact An impact modifier, where 0 is no impact and 1 is the default impact
	  * @param direction Direction that determines whether this color becomes darker or lighter
	  * @return Modified copy of this color
	  */
	def highlightedBy(impact: Double, direction: ColorShade) = direction match {
		case Light => lightenedBy(impact)
		case Dark => darkenedBy(impact)
	}
}
