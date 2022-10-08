package utopia.paradigm.color

import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.DoubleType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.operator.ApproxEquals
import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.angular.Angle
import utopia.paradigm.generic.ParadigmDataType.{AngleType, HslType}
import utopia.paradigm.generic.ParadigmValue._

import scala.language.implicitConversions

object Hsl extends FromModelFactoryWithSchema[Hsl]
{
	// ATTRIBUTES   ----------------
	
	override val schema = ModelDeclaration(Vector(
		PropertyDeclaration("hue", AngleType, Vector("h")),
		PropertyDeclaration("saturation", DoubleType, Vector("s")),
		PropertyDeclaration("luminosity", DoubleType, Vector("luminance", "l"))
	))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Implicitly converts a hsl color to color
	  * @param hsl A hsl color
	  * @return A color
	  */
	implicit def hslToColor(hsl: Hsl): Color = Color(Left(hsl), 1.0)
	
	
	// IMPLEMENTED  ----------------
	
	override protected def fromValidatedModel(model: Model) =
		apply(model("hue").getAngle, model("saturation").getDouble, model("luminosity").getDouble)
	
	
	// OPERATORS	----------------
	
	/**
	  * Creates a new HSL color
	  * @param hue Color hue [0, 360[ where 0 is red, 120 is green and 240 is blue
	  * @param saturation Saturation [0, 1] where 0 is grayscale and 1 is fully saturated
	  * @param luminosity Luminosity [0, 1] where 0 is black and 1 is white
	  * @return A new HSL color
	  */
	def apply(hue: Angle, saturation: Double, luminosity: Double): Hsl =
	{
		val s = 0.0 max saturation min 1.0
		val l = 0.0 max luminosity min 1.0
		
		new Hsl(hue, s, l)
	}
}

/**
  * A HSL represents a color value with hue, satruation and luminance
  * @author Mikko Hilpinen
  * @since Genesis 24.4.2019, v1+
  * @param hue Color hue [0, 360[ where 0 is red, 120 is green and 240 is blue
  * @param saturation Color saturation [0, 1] where 0 is grayscale and 1 is fully saturated
  * @param luminosity Color luminosity [0, 1] where 0 is black and 1 is white
  */
case class Hsl private(override val hue: Angle, override val saturation: Double, override val luminosity: Double)
	extends HslLike[Hsl] with ApproxEquals[HslLike[_]] with ValueConvertible with ModelConvertible
{
	// COMPUTED	------------------
	
	/**
	  * @return An RGB representation of this color
	  */
	def toRGB =
	{
		//  Formula needs all values between 0 - 1.
		val h = hue.degrees / 360
		
		val q =
		{
			if (luminosity < 0.5)
				luminosity * (1 + saturation)
			else
				(luminosity + saturation) - (saturation * luminosity)
		}
		
		val p = 2 * luminosity - q
		
		val r = hueToRGB(p, q, h + (1.0 / 3.0))
		val g = hueToRGB(p, q, h)
		val b = hueToRGB(p, q, h - (1.0 / 3.0))
		
		Rgb(r, g, b)
	}
	
	
	// IMPLEMENTED	--------------
	
	override implicit def toValue: Value = new Value(Some(this), HslType)
	
	override def toModel = Model.from("hue" -> hue, "saturation" -> saturation, "luminosity" -> luminosity)
	
	/**
	  * Checks whether the two instances are approximately equal
	  */
	override def ~==(other: HslLike[_]) = (hue ~== other.hue) &&
		(saturation ~== other.saturation) && (luminosity ~== other.luminosity)
	
	/**
	  * @param hue New hue [0, 360[
	  * @return A copy of this color with new hue
	  */
	def withHue(hue: Angle) = Hsl.apply(hue, saturation, luminosity)
	
	/**
	  * @param saturation New saturation [0, 1]
	  * @return A copy of this color with new saturation
	  */
	def withSaturation(saturation: Double) = Hsl.apply(hue, saturation, luminosity)
	
	/**
	  * @param luminosity New luminosity [0, 1]
	  * @return A copy of this color with new luminosity
	  */
	def withLuminosity(luminosity: Double) = Hsl.apply(hue, saturation, luminosity)
	
	override def toString = s"Hue: $hue, Saturation: $saturationPercent%, Luminosity: $luminosityPercent%"
	
	
	// OTHER	------------------
	
	private def hueToRGB(p: Double, q: Double, h0: Double) =
	{
		// Sets h to range 0-1
		val h = if (h0 < 0) h0 + 1 else if (h0 > 1) h0 - 1 else h0
		
		if (6 * h < 1)
			p + ((q - p) * 6 * h)
		else if (2 * h < 1 )
			q
		else if (3 * h < 2)
			p + ( (q - p) * 6 * ((2.0 / 3.0) - h) )
		else
			p
	}
}
