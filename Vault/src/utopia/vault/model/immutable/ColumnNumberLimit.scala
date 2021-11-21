package utopia.vault.model.immutable
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._

/**
  * A column length limit that handles number (integer) values
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
trait ColumnNumberLimit extends ColumnLengthLimit
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The maximum value allowed by this specific limit (inclusive). May be smaller than type max value.
	  */
	def maxValue: Long
	
	
	// IMPLEMENTED  ----------------------
	
	override def test(input: Value, allowCrop: Boolean = false) =
		input.long match {
			case Some(number) =>
				if (number <= maxValue && number >= -maxValue)
					Right(input)
				else if (allowCrop) {
					if (number >= 0)
						Right(maxValue)
					else
						Right(-maxValue)
				}
				else
					Left(ColumnNumberLimit.fitting(number))
			case None => Left(None)
		}
}

case class SpecificNumberLimit(maxValue: Long, sqlType: String) extends ColumnNumberLimit

trait GeneralColumnNumberLimit
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Maximum value allowed by this limit
	  */
	def maxValue: Long
	/**
	  * @return Maximum number of characters that fit into this limit
	  */
	def maxLength: Int
	
	/**
	  * @return An sql data type (as string) represented / used by this limit
	  */
	def sqlType: String
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param maxLength A specific maximum length to apply - as number of characters (optional)
	  * @return A specific version of this limit
	  */
	def apply(maxLength: Int = maxLength) =
	{
		if (maxLength >= this.maxLength)
			SpecificNumberLimit(maxValue, sqlType)
		else
			SpecificNumberLimit(maxValue min maxValueByLength(maxLength), s"$sqlType($maxLength)")
	}
	/**
	  * @param maxLength A specific maximum length to apply - as number of characters (optional)
	  * @return A specific version of this limit
	  */
	def apply(maxLength: Option[Int]): SpecificNumberLimit = maxLength match {
		case Some(maxLength) => apply(maxLength)
		case None => SpecificNumberLimit(maxValue, sqlType)
	}
	
	/**
	  * @param value Value to fit
	  * @return A number limit of this type that fits the specified value. None if this category can't fit that value.
	  */
	def fitting(value: Long) = {
		val absValue = value.abs
		if (absValue <= maxValue)
			(1 to maxLength).find { maxValueByLength(_) >= value }.map { maxLength => apply(maxLength) }
		else
			None
	}
	
	/**
	  * @param maxLength Maximum value length as characters
	  * @return Numeric maximum value
	  */
	// 1 => 9; 2 => 99; 3 => 999; ...
	protected def maxValueByLength(maxLength: Int) =
		(0 until maxLength).map { index => 9 * math.pow(10, index).toInt }.sum
}

object ColumnNumberLimit
{
	// COMPUTED -----------------------------
	
	/**
	  * @return All column number limit types listed here
	  */
	def categories = Vector(TinyIntLimit, SmallIntLimit, MediumIntLimit, IntLimit, BigIntLimit)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param value A value to fit
	  * @return A number limit that accepts the specified value. None if none of the listed types accepts that value.
	  */
	def fitting(value: Long) = categories.findMap { _.fitting(value) }
	
	
	// NESTED   -----------------------------
	
	/**
	  * An (unsigned) tiny int, with values up to 127
	  */
	case object TinyIntLimit extends GeneralColumnNumberLimit
	{
		override def maxValue = 127
		override def maxLength = 3
		override def sqlType = "TINYINT"
	}
	/**
	  * An (unsigned) small int, with values up to 32 767
	  */
	case object SmallIntLimit extends GeneralColumnNumberLimit
	{
		override def maxValue = 32767
		override def maxLength = 5
		override def sqlType = "SMALLINT"
	}
	/**
	  * An (unsigned) medium int, with values up to 8 388 607
	  */
	case object MediumIntLimit extends GeneralColumnNumberLimit
	{
		override def maxValue = 8388607
		override def maxLength = 7
		override def sqlType = "MEDIUMINT"
	}
	/**
	  * A standard integer, with values up to 2 147 483 647
	  */
	case object IntLimit extends GeneralColumnNumberLimit
	{
		override def maxValue = 2147483647
		override def maxLength = 11
		override def sqlType = "INT"
	}
	/**
	  * A big integer, with values up to 9 223 372 036 854 775 807 (long numbers)
	  */
	case object BigIntLimit extends GeneralColumnNumberLimit
	{
		override def maxValue = 9223372036854775807L
		override def maxLength = 19
		override def sqlType = "BIGINT"
	}
}
