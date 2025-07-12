package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.logging.Logger
import utopia.flow.util.TryExtensions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column

import scala.util.Try

object AccessColumnValues
{
	// OTHER    --------------------------
	
	/**
	  * Creates an access point to an individual column's values
	  * @param access Access point used for accessing column values
	  * @param column Targeted column
	  * @return A new access point
	  */
	def apply(access: AccessManyColumns, column: Column) = new AccessColumnValuesFactory(access, column)
		
	
	// NESTED   -------------------------
	
	class AccessColumnValuesFactory(access: AccessManyColumns, column: Column)
		extends ColumnValueAccessFactory[AccessColumnValues]
	{
		// IMPLEMENTED  -----------------
		
		override def customInput[A, I](parse: Value => A)(toValue: I => Value) =
			_apply(Right(parse))(toValue)
		
		
		// OTHER    ---------------------
		
		/**
		 * Creates an access point to an individual column's values. Assumes that each column contains 0-n values.
		 * @param f A function that parses a column value into 0-n instances of the desired data type
		 * @param valueOf Implicit function that converts an input value into a value to store
		 * @tparam V Type individual output values
		 * @return A new access point
		 */
		def flatten[V](f: Value => IterableOnce[V])(implicit valueOf: V => Value) =
			_apply(Left(f))(valueOf)
		/**
		 * Creates an access point to an individual column's values. Assumes that each column contains 0-n values.
		 * Uses a custom to-value function.
		 * @param parse A function that parses a column value into 0-n instances of the desired data type
		 * @param toValue A function that converts an input value into a value to store
		 * @tparam A Type of individual output values
		 * @tparam I Type of accepted input
		 * @return A new access point
		 */
		def flattenCustomInput[A, I](parse: Value => IterableOnce[A])(toValue: I => Value) =
			_apply(Left(parse))(toValue)
		
		/**
		 * Creates an access point that handles parse failures by logging them
		 * @param f A function that parses a column value into the desired data type. May yield a failure.
		 * @param valueOf Implicit function that converts an input value into a value to store
		 * @param log Implicit logging implementation to use
		 * @tparam V Type of successfully parsed values
		 * @return A new access point
		 */
		def logging[V](f: Value => Try[V])(implicit valueOf: V => Value, log: Logger) =
			loggingWith(log)(f)
		/**
		 * Creates an access point that handles parse failures by logging them
		 * @param log A logging implementation to use
		 * @param f A function that parses a column value into the desired data type. May yield a failure.
		 * @param valueOf Implicit function that converts an input value into a value to store
		 * @tparam V Type of successfully parsed values
		 * @return A new access point
		 */
		def loggingWith[V](log: Logger)(f: Value => Try[V])(implicit valueOf: V => Value) =
			log.use { implicit log =>
				_apply[V, V](Left(v => f(v).logWithMessage(s"Failed to parse a ${ column.columnName } value")))(valueOf)
			}
		
		private def _apply[A, I](parse: Either[Value => IterableOnce[A], Value => A])(toValue: I => Value) =
			new AccessColumnValues[A, I](access, column)(parse)(toValue)
	}
}

/**
  * An interface that provides access to a single column's values
  * @tparam A Type of parsed column value
  * @tparam In Type of accepted input when assigning values
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
class AccessColumnValues[+A, -In](override protected val access: AccessManyColumns, override val column: Column)
                                 (val fromValue: Either[Value => IterableOnce[A], Value => A])
                                 (toValue: In => Value)
	extends ColumnValueAccess[Seq[Value], Seq[A], In]
{
	// COMPUTED ------------------
	
	/**
	  * @param connection Implicit DB connection
	  * @return Distinct accessible values of this column
	  */
	def distinct(implicit connection: Connection) = parse(access(column, distinct = true))
	/**
	 * Maps column values to row ids.
	 * Assumes that each row contains a non-empty index, and that 'fromValue' yields 0-1 items.
	  * @param connection Implicit DB connection
	  * @return Individual values of this column mapped to the primary index of the matching row
	  */
	def byId(implicit connection: Connection) = {
		val values = access(access.index, column)
		fromValue match {
			// Case: Processing optional values => Uses flatMap
			case Left(flatMap) =>
				values.iterator
					.flatMap { vals => vals.lift(1).iterator.flatMap(flatMap).map { v => vals.head.getInt -> v } }
					.toMap
				
			// Case: Processing non-optional values => Uses map
			case Right(map) => values.iterator.map { vals => vals.head.getInt -> map(vals(1)) }.toMap
		}
	}
	
	
	// IMPLEMENTED  --------------
	
	override protected def parse(value: Seq[Value]): Seq[A] = fromValue match {
		case Left(flatMap) => value.flatMap(flatMap)
		case Right(map) => value.map(map)
	}
	override protected def valueOf(value: In): Value = toValue(value)
	
	
	// OTHER    -----------------
	
	/**
	  * Streams accessible values of this column
	  * @param f A function for processing streamed values
	  * @param connection Implicit DB connection
	  * @tparam B Function result type
	  * @return Function 'f' results
	  */
	def stream[B](f: Iterator[A] => B)(implicit connection: Connection) = _stream(f, distinct = false)
	/**
	  * Streams accessible distinct values of this column
	  * @param f A function for processing streamed values
	  * @param connection Implicit DB connection
	  * @tparam B Function result type
	  * @return Function 'f' results
	  */
	def streamDistinct[B](f: Iterator[A] => B)(implicit connection: Connection) = _stream(f, distinct = true)
	
	private def _stream[B](f: Iterator[A] => B, distinct: Boolean)(implicit connection: Connection) =
		access.streamColumn(column, distinct) { valuesIter =>
			fromValue match {
				case Left(flatMap) => f(valuesIter.flatMap(flatMap))
				case Right(map) => f(valuesIter.map(map))
			}
		}
}
