package utopia.paradigm.measurement

import utopia.flow.operator.{HasExtremes, Steppable}
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.sign.Sign

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
	
	
	// VALUES   ---------------------------
	
	/**
	  * The default unit with the multiplier of 1. E.g. "meter".
	  */
	case object Default extends MetricScale
	{
		override def multiplier: Double = 1.0
		override def prefix: String = ""
		
		override protected def index: Int = 4
	}
	
	/**
	  * A smaller unit that represents 1/10
	  */
	case object Deci extends MetricScale
	{
		override def multiplier: Double = 0.1
		override def prefix: String = "d"
		
		override protected def index: Int = 3
	}
	/**
	  * A smaller unit that represents 1/100
	  */
	case object Centi extends MetricScale
	{
		override def multiplier: Double = 0.01
		override def prefix: String = "c"
		
		override protected def index: Int = 2
	}
	/**
	  * A smaller unit that represents 1/1000
	  */
	case object Milli extends MetricScale
	{
		override def multiplier: Double = 0.001
		override def prefix: String = "m"
		
		override protected def index: Int = 1
	}
	/**
	  * A very small unit that represents 1/1000/1000
	  */
	case object Micro extends MetricScale
	{
		override def multiplier: Double = 0.000001
		override def prefix: String = "μ"
		
		override protected def index: Int = 0
	}
	
	/**
	  * A unit which represents 1000 of something
	  */
	case object Kilo extends MetricScale
	{
		override def multiplier: Double = 1000
		override def prefix: String = "k"
		
		override protected def index: Int = 5
	}
	/**
	  * A unit that represents a million of something
	  */
	case object Mega extends MetricScale
	{
		override def multiplier: Double = 1000000
		override def prefix: String = "M"
		
		override protected def index: Int = 6
	}
}
