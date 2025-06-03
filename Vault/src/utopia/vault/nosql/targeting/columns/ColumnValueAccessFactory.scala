package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Identity

/**
  * Common trait for factories used for constructing column value access points
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
trait ColumnValueAccessFactory[+A[_, _]]
{
	// ABSTRACT ------------------------
	
	/**
	  * Creates an access point to an individual column's values
	  * @param parse A function that parses the column values into the desired data type
	  * @param toValue A function that converts an input value into a value to store
	  * @tparam O Type of parsed column value
	  * @return A new access point
	  */
	def customInput[O, I](parse: Value => O)(toValue: I => Value): A[O, I]
		
	
	// OTHER    ------------------------
	
	/**
	  * Creates an access point to an individual column's values
	  * @param f A function that parses the column values into the desired data type
	  * @param valueOf Implicit function that converts an input value into a value to store
	  * @tparam V Type of parsed column value
	  * @return A new access point
	  */
	def apply[V](f: Value => V)(implicit valueOf: V => Value) = customInput[V, V](f)(valueOf)
	/**
	 * Creates an access point to an individual column's values. Yields optional values.
	 * @param f A function that parses the column values into the desired data type
	 * @param valueOf Implicit function that converts an input value into a value to store
	 * @tparam V Type of parsed column value
	 * @return A new access point
	 */
	def optional[V](f: Value => Option[V])(implicit valueOf: V => Value) =
		customInput[Option[V], V](f)(valueOf)
	/**
	  * Creates an access point to an individual column's values.
	  * @param f A function that parses the column values into the desired data type
	  * @tparam O Type of parsed column value
	  * @return A new access point
	  */
	def noAssign[O](f: Value => O) = customInput[O, Value](f)(Identity)
}
