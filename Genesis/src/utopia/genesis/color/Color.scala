package utopia.genesis.color

import utopia.flow.datastructure.mutable.Pointer
import utopia.genesis.shape.shape1D.Angle
import utopia.genesis.util.ApproximatelyEquatable

import scala.language.implicitConversions
import utopia.genesis.util.Extensions._

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
	implicit def fromAwt(awtColor: java.awt.Color): Color = Color(Right(Rgb.withValues(awtColor.getRed,
		awtColor.getGreen, awtColor.getBlue)), awtColor.getAlpha / 255.0)
	
	
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
	def fromInt(rgba: Int) = fromAwt(new java.awt.Color(rgba, true))
	
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
	def average(colors: Iterable[Color]) =
	{
		if (colors.isEmpty)
			transparentBlack
		else
		{
			// Combines the total rgb and alpha values of the colors (weights by color alpha)
			val totals = RgbChannel.values.map { _ -> new Pointer(0.0) }.toMap
			var totalAlpha = 0.0
			colors.foreach { color =>
				color.ratios.foreach { case (channel, ratio) => totals(channel).update { _ + ratio * color.alpha } }
				totalAlpha += color.alpha
			}
			// Calculates the average value
			if (totalAlpha == 0.0)
				transparentBlack
			else
				apply(Rgb.withRatios(totals.view.mapValues { _.value / totalAlpha }.toMap), totalAlpha / colors.size)
		}
	}
	
	/**
	  * @param colors A collection of colors
	  * @return The average luminosity of those colors
	  */
	def averageLuminosityOf(colors: IterableOnce[Color]) = colors.iterator
		.map { c => (c.luminosity * c.alpha) -> c.alpha }
		.reduceOption { (a, b) => (a._1 + b._1) -> (a._2 + b._2) } match
		{
			case Some((totalLuminosity, totalAlpha)) => totalLuminosity / totalAlpha
			case None => 0.0
		}
}

/**
  * This class represents color with either RGB or HSL
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
case class Color private(private val data: Either[Hsl, Rgb], alpha: Double) extends RgbLike[Color] with HslLike[Color]
	with ApproximatelyEquatable[Color]
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
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Whether this color has transparency
	  */
	def isTransparent = alpha < 1
	
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
	
	
	// IMPLEMENTED	----------------------
	
	override def ~==(other: Color) =
	{
		// Alphas must match
		if (alpha ~== other.alpha)
		{
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
	
	override def toString =
	{
		val base = data.fold(_.toString, _.toString)
		if (isTransparent)
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
	def average(other: Color) =
	{
		if (other.alpha == 0.0)
		{
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
	def average(other: Color, weight: Double) =
	{
		def newAlpha = (alpha * weight + other.alpha) / (1 + weight)
		if (other.alpha == 0.0)
		{
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
}
