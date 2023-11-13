package utopia.vault.database.columnlength

import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.ordering.SelfComparable

/**
  * Used for tracking column maximum values
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
trait ColumnLengthLimit extends SelfComparable[ColumnLengthLimit]
{
	// ABSTRACT ---------------------------------
	
	/**
	  * @return This type & limit as an sql data type. E.g. String length limit of 32 = "VARCHAR(32)"
	  */
	def sqlType: String
	
	/**
	  * @return The maximum length allowed by this limit. The exact meaning of this value may be context-specific
	  *         (e.g. different for text and numbers)
	  */
	def maxValue: Long
	
	/**
	  * Tests a value, whether it fits into this limit
	  * @param input     Input value
	  * @param allowCrop Whether it is allowed to crop the value to fit within this length (default = false)
	  * @return Either Left: A column length required to fit the specified value or
	  *         None if such length couldn't be determined. Or Right: A value which fits into this length limit.
	  */
	def test(input: Value, allowCrop: Boolean = false): Either[Option[ColumnLengthLimit], Value]
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self = this
	
	override def compareTo(o: ColumnLengthLimit) = maxValue.compareTo(o.maxValue)
}
