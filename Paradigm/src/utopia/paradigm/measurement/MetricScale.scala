package utopia.paradigm.measurement

import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.{HasExtremes, Steppable}
import utopia.flow.util.StringExtensions._

/**
  * An enumeration for different 10-based size units in the metric system
  * @author Mikko Hilpinen
  * @since 8.12.2023, v1.5
  */
sealed trait MetricScale extends Steppable[MetricScale]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return A multiplier applied to values in this scale when converting them back to the default scale / unit.
	  */
	def multiplier: Double
	/**
	  * @return The prefix added before the unit abbreviation.
	  *         E.g. The abbreviation in km (kilometer) is k.
	  */
	def prefix: String
	
	/**
	  * @return The index of this scale in the MetricScale.values vector
	  */
	protected def index: Int
	
	
	// IMPLEMENTED  ------------------------
	
	override def self: MetricScale = this
	
	override def next(direction: Sign): MetricScale =
		MetricScale.values.lift(index + direction.modifier).getOrElse(this)
	override def is(extreme: Extreme): Boolean = this == MetricScale(extreme)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param scale Another scale
	  * @return A modifier that needs to be applied to a value from the other scale in order to get a value in this scale
	  */
	def modifierFrom(scale: MetricScale) = scale.multiplier / multiplier
	/**
	  * @param scale Another scale
	  * @return A modifier that needs to be applied to a value from this scale in order to get a value in the other scale
	  */
	def modifierTo(scale: MetricScale) = multiplier / scale.multiplier
}

object MetricScale extends HasExtremes[MetricScale]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * All predefined (in this context) metric scale values, from the smallest to the largest
	  */
	val values = Vector[MetricScale](Micro, Milli, Centi, Deci, Default, Kilo, Mega)
	
	
	// IMPLEMENTED  -----------------------
	
	/**
	  * @param extreme Targeted extreme
	  * @return The most extreme unit in that direction
	  */
	override def apply(extreme: Extreme): MetricScale = extreme match {
		case Min => Micro
		case Max => Mega
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param value A value in the default unit
	  * @return The scale that is most readable with the specified value's size
	  */
	def appropriateFor(value: Double) =
		values.reverseIterator.find { value >= _.multiplier }.getOrElse(min)
	
	/**
	  * @param abbreviation A unit abbreviation, e.g. Km.
	  *                     Note: Not a full string, like "kilometer or micrometer"
	  * @return Metric scale mentioned within the specified abbreviation.
	  *         Default if no other scale is mentioned.
	  */
	def forAbbreviation(abbreviation: String) =
		values.find { s => s != Default && abbreviation.startsWithIgnoreCase(s.prefix) }
			.getOrElse(Default)
	/**
	  * Finds the scale part from the specified string
	  * @param unitString A string representing a scale, plus some unit
	  * @return Scale that was mentioned, plus the remaining part of the unit string
	  */
	def from(unitString: String) =
		values.find { s => s != Default && unitString.startsWithIgnoreCase(s.toString) } match {
			case Some(scale) => scale -> unitString.drop(scale.toString.length)
			case None =>
				val scale = forAbbreviation(unitString)
				scale -> unitString.drop(scale.prefix.length)
		}
	
	
	// VALUES   ---------------------------
	
	/**
	  * The default unit with the multiplier of 1. E.g. "meter".
	  */
	case object Default extends MetricScale
	{
		override val multiplier: Double = 1.0
		override val prefix: String = ""
		
		override protected val index: Int = 4
	}
	
	/**
	  * A smaller unit that represents 1/10
	  */
	case object Deci extends MetricScale
	{
		override val multiplier: Double = 0.1
		override val prefix: String = "d"
		
		override protected val index: Int = 3
	}
	/**
	  * A smaller unit that represents 1/100
	  */
	case object Centi extends MetricScale
	{
		override val multiplier: Double = 0.01
		override val prefix: String = "c"
		
		override protected val index: Int = 2
	}
	/**
	  * A smaller unit that represents 1/1000
	  */
	case object Milli extends MetricScale
	{
		override val multiplier: Double = 0.001
		override val prefix: String = "m"
		
		override protected val index: Int = 1
	}
	/**
	  * A very small unit that represents 1/1000/1000
	  */
	case object Micro extends MetricScale
	{
		override val multiplier: Double = 0.000001
		override val prefix: String = "μ"
		
		override protected val index: Int = 0
	}
	
	/**
	  * A unit which represents 1000 of something
	  */
	case object Kilo extends MetricScale
	{
		override val multiplier: Double = 1000
		override val prefix: String = "k"
		
		override protected val index: Int = 5
	}
	/**
	  * A unit that represents a million of something
	  */
	case object Mega extends MetricScale
	{
		override val multiplier: Double = 1000000
		override val prefix: String = "M"
		
		override protected val index: Int = 6
	}
}
