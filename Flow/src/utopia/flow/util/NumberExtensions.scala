package utopia.flow.util

import utopia.flow.collection.immutable.range.NumericSpan

/**
  * Provides additional functions for numbers
  * @author Mikko Hilpinen
  * @since 4.3.2023, v2.1
  */
object NumberExtensions
{
	implicit class RichDouble(val d: Double) extends AnyVal
	{
		/**
		  * Rounds this number to n or less decimal places
		  * @param maxDecimals Maximum number of decimal places allowed in the resulting value
		  * @return A rounded copy of this value
		  */
		def roundDecimals(maxDecimals: Int) = {
			val mod = math.pow(10, maxDecimals)
			(d * mod).round / mod
		}
		
		/**
		  * @param other Another double number
		  * @return A span from this number to the other number
		  */
		def to(other: Double) = NumericSpan(d, other)
	}
}
