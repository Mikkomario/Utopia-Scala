package utopia.vault.model.immutable

import utopia.flow.datastructure.immutable.Value

/**
  * Used for tracking column maximum values
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
trait ColumnLengthLimit
{
	/**
	  * @return This type & limit as an sql data type. E.g. String length limit of 32 = "VARCHAR(32)"
	  */
	def sqlType: String
	
	/**
	  * Tests a value, whether it fits into this limit
	  * @param input Input value
	  * @param allowCrop Whether it is allowed to crop the value to fit within this length (default = false)
	  * @return Either Left: A column length required to fit the specified value or
	  *         None if such length couldn't be determined. Or Right: A value which fits into this length limit.
	  */
	def test(input: Value, allowCrop: Boolean = false): Either[Option[ColumnLengthLimit], Value]
}