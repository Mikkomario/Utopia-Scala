package utopia.vault.nosql.targeting.columns

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
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
	{
		// OTHER    ---------------------
		
		/**
		 * Creates an access point to an individual column's values
		 * @param f A function that parses the column values into the desired data type
		 * @param valueOf Implicit function that converts an input value into a value to store
		 * @tparam V Type of parsed column value
		 * @return A new access point
		 */
		def apply[V](f: Value => V)(implicit valueOf: V => Value) =
			customInput[V, V](f)(valueOf)
		/**
		 * Creates an access point to an individual column's values
		 * @param parse A function that parses the column values into the desired data type
		 * @param toValue A function that converts an input value into a value to store
		 * @tparam A Type of parsed column value
		 * @return A new access point
		 */
		def customInput[A, I](parse: Value => A)(toValue: I => Value) =
			_apply(Right(parse))(toValue)
		
		/**
		 * Creates an access point to an individual column's values.
		 * @param f A function that parses the column values into the desired data type
		 * @tparam O Type of parsed column value
		 * @return A new access point
		 */
		def noAssign[O](f: Value => O) = customInput[O, Value](f)(Identity)
		
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
	
	
	// EXTENSIONS   ---------------------------
	
	implicit class AccessColumnIntValues(val a: AccessColumnValues[Int, _]) extends AnyVal
	{
		/**
		 * @param connection Implicit DB connection
		 * @return All accessible values as an [[IntSet]]
		 */
		def toIntSet(implicit connection: Connection) = a.streamDistinct(IntSet.from)
	}
}

/**
  * An interface that provides access to a single column's values
  * @tparam A Type of a parsed (concrete) column value
  * @tparam In Type of accepted input when assigning values
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
class AccessColumnValues[+A, -In](override protected val access: AccessManyColumns, override val column: Column)
                                 (val fromValue: Either[Value => IterableOnce[A], Value => A])
                                 (toValue: In => Value)
	extends ColumnValueAccess[Seq[Value], Seq[A], A, In]
{
	// COMPUTED ------------------
	
	/**
	  * @param connection Implicit DB connection
	  * @return The smallest accessible value
	  */
	@throws[NoSuchElementException]("If value-mapping yielded 0 values")
	def min(implicit connection: Connection) = apply(Min)
	/**
	  * @param connection Implicit DB connection
	  * @return The largest accessible value
	  */
	@throws[NoSuchElementException]("If value-mapping yielded 0 values")
	def max(implicit connection: Connection) = apply(Max)
	/**
	  * @param connection Implicit DB connection
	  * @return The smallest accessible value. None if value-mapping didn't yield any values.
	  */
	def minOption(implicit connection: Connection) = find(Min)
	/**
	  * @param connection Implicit DB connection
	  * @return The largest accessible value. None if value-mapping didn't yield any values.
	  */
	def maxOption(implicit connection: Connection) = find(Max)
	
	/**
	  * @param connection Implicit DB connection
	  * @return Distinct accessible values of this column
	  */
	def distinct(implicit connection: Connection) = parse(access(column, distinct = true))
	/**
	 * @param connection Implicit DB connection
	 * @return If this column only contains a single distinct accessible value, yields that.
	 *         Otherwise, yields None.
	 */
	def only(implicit connection: Connection) =
		streamDistinct { iter => iter.nextOption().filterNot { _ => iter.hasNext } }
	
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
	
	/**
	 * @param connection Implicit DB connection
	 * @tparam B Type of the collected elements
	 * @return A set containing all distinct accessible values
	 */
	def toSet[B >: A](implicit connection: Connection) = streamDistinct { _.toSet[B] }
	
	
	// IMPLEMENTED  --------------
	
	/**
	 * Streams accessible values of this column
	 * @param f A function for processing streamed values
	 * @param connection Implicit DB connection
	 * @tparam B Function result type
	 * @return Function 'f' results
	 */
	override def stream[B](f: Iterator[A] => B)(implicit connection: Connection) = _stream(f, distinct = false)
	
	override protected def parse(value: Seq[Value]): Seq[A] = fromValue match {
		case Left(flatMap) => value.flatMap(flatMap)
		case Right(map) => value.map(map)
	}
	override protected def valueOf(value: In): Value = toValue(value)
	
	
	// OTHER    -----------------
	
	/**
	  * @param extreme Targeted extreme
	  * @param connection Implicit DB connection
	  * @return The most extreme accessible value
	  */
	@throws[NoSuchElementException]("If value-mapping yielded 0 values")
	def apply(extreme: Extreme)(implicit connection: Connection) = {
		val value = access(column, extreme)
		fromValue match {
			case Left(flatMap) =>
				flatMap(value).iterator.nextOption().getOrElse { throw new NoSuchElementException(
					s"No $extreme value could be extracted from ${ value.description }") }
			case Right(map) => map(value)
		}
	}
	/**
	  * @param extreme Targeted extreme
	  * @param connection Implicit DB connection
	  * @return The most extreme accessible value.
	  *         None if no values were accessible, or if value-mapping didn't yield any values.
	  */
	def find(extreme: Extreme)(implicit connection: Connection) =
		access(column, extreme).notEmpty.flatMap { value =>
			fromValue match {
				case Left(flatMap) => flatMap(value).iterator.nextOption()
				case Right(map) => Some(map(value))
			}
		}
	
	def reduce[B >: A](f: (B, B) => B)(implicit connection: Connection) = stream { _.reduce(f) }
	def reduceOption[B >: A](f: (B, B) => B)(implicit connection: Connection) = stream { _.reduceOption(f) }
	
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
