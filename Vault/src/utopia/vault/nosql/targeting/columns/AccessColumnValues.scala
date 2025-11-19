package utopia.vault.nosql.targeting.columns

import utopia.flow.collection.immutable.{Empty, IntSet}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.sql.OrderDirection
import utopia.vault.sql.OrderDirection.{Ascending, Descending}

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
		def toIntSet(implicit connection: Connection) = a.streamOrdered(Ascending, distinct = true)(IntSet.fromOrdered)
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
	 * @param connection Implicit DB connection
	 * @return Number of accessible values of this column
	 */
	def size(implicit connection: Connection) = access.count(column)
	/**
	 * @param connection Implicit DB connection
	 * @return Number of accessible distinct values of this column
	 */
	def distinctSize(implicit connection: Connection) = access.count(column, distinct = true)
	
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
	override def stream[B](f: Iterator[A] => B)(implicit connection: Connection) = _stream(f)
	
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
	
	/**
	 * Collects 'count' smallest accessible values
	 * @param count Maximum number of values to collect
	 * @param connection Implicit DB connection
	 * @return Up to 'count' smallest accessible values. Ordered.
	 */
	def takeMin(count: Int)(implicit connection: Connection) = take(Min, count)
	/**
	 * Collects 'count' largest accessible values
	 * @param count Maximum number of values to collect
	 * @param connection Implicit DB connection
	 * @return Up to 'count' largest accessible values. Ordered.
	 */
	def takeMax(count: Int)(implicit connection: Connection) = take(Max, count)
	/**
	 * Collects 'count' most extreme accessible values
	 * @param extreme Targeted extreme
	 * @param count Maximum number of values to collect
	 * @param connection Implicit DB connection
	 * @return Up to 'count' most extreme accessible values. Ordered.
	 */
	def take(extreme: Extreme, count: Int)(implicit connection: Connection) = {
		if (count <= 0)
			Empty
		else
			streamOrdered(OrderDirection.startingFrom(extreme)) { _.take(count).toOptimizedSeq }
	}
	
	/**
	 * Reduces the accessible column values using the specified function
	 * @param f A reduce function, which combines two accessible column values
	 * @param connection Implicit DB connection
	 * @tparam B Type of the combined values
	 * @return Reduce result
	 */
	@throws[UnsupportedOperationException]("If no value is accessible")
	def reduce[B >: A](f: (B, B) => B)(implicit connection: Connection) = stream { _.reduce(f) }
	/**
	 * Reduces the accessible column values using the specified function
	 * @param f A reduce function, which combines two accessible column values
	 * @param connection Implicit DB connection
	 * @tparam B Type of the combined values
	 * @return Reduce result. None if no values were accessible.
	 */
	def reduceOption[B >: A](f: (B, B) => B)(implicit connection: Connection) = stream { _.reduceOption(f) }
	
	/**
	  * Streams accessible distinct values of this column
	  * @param f A function for processing streamed values
	  * @param connection Implicit DB connection
	  * @tparam B Function result type
	  * @return Function 'f' results
	  */
	def streamDistinct[B](f: Iterator[A] => B)(implicit connection: Connection) = _stream(f, distinct = true)
	
	/**
	 * Streams accessible values of this column in ascending order
	 * @param f A function that receives a value iterator and yields some value
	 * @param connection Implicit DB connection
	 * @tparam B Type of the value yielded by 'f'
	 * @return Result of 'f'
	 */
	def streamAscending[B](f: Iterator[A] => B)(implicit connection: Connection) = streamOrdered(Ascending)(f)
	/**
	 * Streams accessible values of this column in descending order
	 * @param f A function that receives a value iterator and yields some value
	 * @param connection Implicit DB connection
	 * @tparam B Type of the value yielded by 'f'
	 * @return Result of 'f'
	 */
	def streamDescending[B](f: Iterator[A] => B)(implicit connection: Connection) = streamOrdered(Descending)(f)
	/**
	 * Streams accessible values of this column in order
	 * @param direction Order in which the values are presented to 'f'
	 * @param f A function that receives a value iterator and yields some value
	 * @param distinct Whether to only include distinct values (default = false)
	 * @param connection Implicit DB connection
	 * @tparam B Type of the value yielded by 'f'
	 * @return Result of 'f'
	 */
	def streamOrdered[B](direction: OrderDirection, distinct: Boolean = false)(f: Iterator[A] => B)
	                    (implicit connection: Connection) =
		_stream(f, Some(direction), distinct)
	
	private def _stream[B](f: Iterator[A] => B, order: Option[OrderDirection] = None, distinct: Boolean = false)
	                      (implicit connection: Connection) =
		access.streamColumn(column, order, distinct = distinct) { valuesIter =>
			fromValue match {
				case Left(flatMap) => f(valuesIter.flatMap(flatMap))
				case Right(map) => f(valuesIter.map(map))
			}
		}
}
