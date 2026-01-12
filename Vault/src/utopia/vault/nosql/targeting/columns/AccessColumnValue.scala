package utopia.vault.nosql.targeting.columns

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Identity
import utopia.flow.parse.json.JsonParser
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.columns.AccessColumnValue.AccessColumnValueFactory.AccessGenericColumnValueFactory
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn

import scala.util.Try

object AccessColumnValue
{
	// OTHER    --------------------------
	
	/**
	  * @param access Access point used for accessing column values
	  * @param column Targeted column
	  * @return A factory for constructing the column value access interface
	  */
	def apply(access: AccessColumn, column: Column) = AccessColumnValueFactory(access, column)
	
	
	// NESTED   --------------------------
	
	object AccessColumnValueFactory
	{
		// OTHER    ----------------------
		
		/**
		 * @param access Access point used for accessing column values
		 * @param column Targeted column
		 * @return A factory for constructing the column value access interface
		 */
		def apply(access: AccessColumn, column: Column): AccessColumnValueFactory =
			new _AccessColumnValueFactory(access, column)
		
		
		// NESTED   ----------------------
		
		private class AccessGenericColumnValueFactory(wrapped: AccessColumnValueFactory)
		                                             (implicit jsonParser: JsonParser)
			extends AccessColumnValueFactory
		{
			override def parsingGenericValues(implicit jsonParser: JsonParser): AccessColumnValueFactory = this
			
			override def custom[O, I](parse: Value => O)(toValue: I => Value): PreparedAccessColumnValueFactory[O, I] =
				wrapped.custom { v =>
					if (v.isEmpty)
						parse(Value.empty)
					else
						parse(jsonParser.valueOf(v.getString))
				}(toValue)
		}
		
		private class _AccessColumnValueFactory(access: AccessColumn, column: Column) extends AccessColumnValueFactory
		{
			def custom[O, I](parse: Value => O)(toValue: I => Value) =
				new PreparedAccessColumnValueFactory[O, I](access, column, parse, toValue)
		}
	}
	
	trait AccessColumnValueFactory
	{
		// ABSTRACT ----------------------
		
		/**
		 * Creates a new column value access
		 * @param parse A function for parsing individual column values into the presented data type
		 * @param toValue A function that converts an input value into a value to update the column with
		 * @tparam O Type of parse function output
		 * @tparam I Type of accepted input
		 * @return A factory for finalizing the accessor into either a concrete or an iterable version
		 */
		def custom[O, I](parse: Value => O)(toValue: I => Value): PreparedAccessColumnValueFactory[O, I]
		
		
		// COMPUTED ----------------------
		
		/**
		 * @param jsonParser Implicit JSON parser used when interpreting the column (String) values
		 * @return A copy of this factory which parses the values into JSON before processing them further.
		 *         E.g. Whereas this factory would present '"A"' (String) or '[1,2,3]' (String)
		 *         as 'parse' function parameters, the resulting factory would yield
		 *         'A' (String) and '[1,2,3]' (Vector).
		 */
		def parsingGenericValues(implicit jsonParser: JsonParser): AccessColumnValueFactory =
			new AccessGenericColumnValueFactory(this)
		
		
		// OTHER    ----------------------
		
		/**
		 * Creates a new column value access. Assumes that the parsed values are concrete, and not collections.
		 * @param parse A function that parses a value into the presented data type
		 * @param toValue An implicit function that converts an input value into a value to update the column with
		 * @tparam A Type of parsed column values; Also the type of accepted input values.
		 * @return A column value accessor
		 */
		def apply[A](parse: Value => A)(implicit toValue: A => Value) =
			custom(parse)(toValue).concrete
		/**
		 * Creates an access point to an individual column's values. Yields optional values.
		 * @param parse A function that parses the column values into the desired data type
		 * @param valueOf Implicit function that converts an input value into a value to store
		 * @tparam A Type of parsed column value
		 * @return A column value accessor
		 */
		def optional[A](parse: Value => Option[A])(implicit valueOf: A => Value) =
			custom(parse)(valueOf).iterable
		/**
		 * Creates an access point to an individual column's values. Yields instances of Try.
		 * @param parse A function for parsing column values
		 * @param valueOf An implicit function that converts an input value into a value to update the column with
		 * @tparam A Type of successfully parsed column values
		 * @return A column value accessor
		 */
		def tryParse[A](parse: Value => Try[A])(implicit valueOf: A => Value) =
			custom(parse)(valueOf).iterateWith { _.toOption }
		
		/**
		 * Creates a new column value access. Doesn't specify a value conversion for column updates.
		 * @param parse A function for parsing individual column values into the presented data type
		 * @tparam A Type of parse function output
		 * @return A factory for finalizing the accessor into either a concrete or an iterable version
		 */
		def noAssign[A](parse: Value => A) =
			custom[A, Value](parse)(Identity)
		
		@deprecated("Please use .custom(...).concrete instead", "v2.0")
		def customInput[O, I](parse: Value => O)(toValue: I => Value): AccessColumnValue[O, O, I] =
			custom(parse)(toValue).concrete
	}
	
	class PreparedAccessColumnValueFactory[+A, -In](access: AccessColumn, column: Column, parse: Value => A,
	                                                toValue: In => Value)
	{
		// COMPUTED ----------------------
		
		/**
		 * Creates an access point to individual column values.
		 * Handles empty values by converting them to concrete instances.
		 * @return A column value accessor
		 */
		def concrete = iterateWith(Single.apply)
		/**
		 * Creates an access point to individual column values. Yields parsed values as collections.
		 * @param ev Implicit evidence for the fact that the parsed type is a collection
		 * @tparam C Type of individual elements within the parsed type
		 * @return A column value accessor
		 */
		def iterable[C](implicit ev: A <:< IterableOnce[C]): AccessColumnValue[A, C, In] = iterateWith[C] { a => a }
		
		
		// OTHER    ---------------------
		
		/**
		 * Creates an access point to individual column's values.
		 * Implements custom value iteration for streaming / iterating functions.
		 * @param iterate A function that converts the parsed value into an iterable format
		 * @tparam C Type of iterated individual values.
		 *
		 *           E.g. if 'A' is Option[Int], this would be Int.
		 * @return A new column value accessor
		 */
		def iterateWith[C](iterate: A => IterableOnce[C]) =
			new AccessColumnValue[A, C, In](access, column)(parse)(iterate)(toValue)
	}
}

/**
  * An interface for accessing individual column values
 *  @tparam A Type of parsed column values
 *  @tparam C Type of concrete (iterable) column values. May be same as 'A'.
 *
 *            E.g. If 'A' is Option[Int], 'C' would be Int. If 'A' is String, 'C' would also be String.
 *
 *  @tparam In Type of accepted input when assigning values
 * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
class AccessColumnValue[+A, +C, -In](override protected val access: AccessColumn, override val column: Column)
                                    (f: Value => A)(iterate: A => IterableOnce[C])(implicit toValue: In => Value)
	extends ColumnValueAccess[Value, A, C, In]
{
	override protected def parse(value: Value): A = f(value)
	override def valueOf(value: In): Value = toValue(value)
	
	override def stream[B](f: Iterator[C] => B)(implicit connection: Connection): B =
		f(iterate(this.f(access(column))).iterator)
}
