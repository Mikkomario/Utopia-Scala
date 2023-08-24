package utopia.flow.operator

/**
  * Common trait for functions used in numeric rounding
  * @author Mikko Hilpinen
  * @since 24.8.2023, v2.2
  */
trait RoundingFunction
{
	/**
	  * @param d A double precision number
	  * @return A rounded copy of that number, according to this function's logic
	  */
	def apply(d: Double): Long
	
	/**
	  * @param d A double precision number
	  * @return A rounded copy of that number, according to this function's logic
	  */
	def toDouble(d: Double): Double
}

object RoundingFunction
{
	/**
	  * A rounding function that rounds to the nearest integer
	  */
	case object Round extends RoundingFunction
	{
		override def apply(d: Double) = d.round
		override def toDouble(d: Double): Double = d.round.toDouble
	}
	/**
	  * A rounding function that rounds to the next full integer
	  */
	case object Ceil extends RoundingFunction
	{
		override def apply(d: Double) = d.ceil.toLong
		override def toDouble(d: Double): Double = d.ceil
	}
	/**
	  * A rounding function that removes the decimal places
	  */
	case object Floor extends RoundingFunction
	{
		override def apply(d: Double) = d.toLong
		override def toDouble(d: Double): Double = d.floor
	}
}