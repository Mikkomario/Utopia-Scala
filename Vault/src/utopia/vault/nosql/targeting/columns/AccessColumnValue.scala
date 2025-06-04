package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn

object AccessColumnValue
{
	// OTHER    --------------------------
	
	/**
	  * @param access Access point used for accessing column values
	  * @param column Targeted column
	  * @return A factory for constructing the column value access interface
	  */
	def apply(access: AccessColumn, column: Column) =  new AccessColumnValueFactory(access, column)
	
	
	// NESTED   --------------------------
	
	class AccessColumnValueFactory(access: AccessColumn, column: Column)
		extends ColumnValueAccessFactory[AccessColumnValue]
	{
		// IMPLEMENTED  ------------------
		
		override def customInput[O, I](parse: Value => O)(toValue: I => Value): AccessColumnValue[O, I] =
			new AccessColumnValue[O, I](access, column)(parse)(toValue)
		
		
		// OTHER    ----------------------
		
		/**
		 * Creates an access point to an individual column's values. Yields optional values.
		 * @param f A function that parses the column values into the desired data type
		 * @param valueOf Implicit function that converts an input value into a value to store
		 * @tparam V Type of parsed column value
		 * @return A new access point
		 */
		def optional[V](f: Value => Option[V])(implicit valueOf: V => Value) =
			customInput[Option[V], V](f)(valueOf)
	}
}

/**
  * An interface for accessing individual column values
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
class AccessColumnValue[+A, -In](override protected val access: AccessColumn, override val column: Column)
                                (f: Value => A)(implicit toValue: In => Value)
	extends ColumnValueAccess[Value, A, In]
{
	override protected def parse(value: Value): A = f(value)
	override protected def valueOf(value: In): Value = toValue(value)
}
